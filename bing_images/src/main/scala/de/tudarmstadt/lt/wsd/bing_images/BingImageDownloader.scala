package de.tudarmstadt.lt.wsd.bing_images

import java.io.{File, FileNotFoundException}
import java.security.MessageDigest

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.slf4j.LazyLogging
import de.tudarmstadt.lt.wsd.common.model.Sense
import de.tudarmstadt.lt.wsd.common.utils.FileUtils
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{StreamedResponse, WSClient, WSRequest, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

/**
  * chmod a+w imgdata/bing
  */
class BingImageDownloader(baseUrl: String) extends LazyLogging {

  def  this() = this("https://api.cognitive.microsoft.com")

  import de.tudarmstadt.lt.wsd.common.JsonImplicits._

  private implicit val photoReads = Json.reads[BingPhoto]
  private val config = ConfigFactory.load()
  private val apiKey = config.getString("wsd.bing_images.api_key")
  private val imageFolder = config.getString("wsd.bing_images.image_folder")
  private val apiEndpoint = baseUrl + "/bing/v5.0/images/search"

  def readCache: Map[String, Option[String]] = {
    val folders = new File(imageFolder).listFiles().flatMap(_.listFiles).map(_.getCanonicalPath)

    val readEntryFromFolder = (folder: String) => (
      readSenseFromFolder(folder).uniqueID,
      readPhotoURLFromFolder(folder)
    )

    folders.map(readEntryFromFolder).toMap
  }

  private def toHash: (String) => String = (str: String) =>
    // 7 digits see to be enough, in such rare cases where senses have the same ascii name
    MessageDigest.getInstance("MD5").digest(str.getBytes).map("%02X" format _).mkString.take(7)

  private def toAscii: (String) => String = (str: String) => {
    val alphaNum = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ List('#', '-')
    str.flatMap{
      case c if alphaNum.contains(c) => Some(c)
      case ' ' => Some('_')
      case _ => None
    }
  }

  /**
    * Create a hierarchical directory structure to avoid too many files in the top
    * level directory.
    *
    * Some performance considerations are discussed in https://stackoverflow.com/a/7032294,
    * especially the `ls` command will suffer form large directories.
    */
  private def bucketPath(sense: Sense): String = {

    val topLevelBucket = if(sense.isInventoryCoset) // Put CoSets into one folder
      sense.inventory
    else
      Try(toAscii(sense.word.toLowerCase()).substring(0, 2)).getOrElse("SHORT_NAME")

    val maxSenseNameLength = 10
    val senseAscii = toAscii(sense.uniqueID).take(maxSenseNameLength)
    val senseMD5 = toHash(sense.uniqueID)
    val senseBucket = s"$senseAscii-$senseMD5"

    s"$imageFolder/$topLevelBucket/$senseBucket"
  }

  private def getQueryText(sense: Sense): String = {
    if (sense.isInventoryCoset)
      s"${sense.hypernyms.take(3).mkString(" ")}"
    else
      s"${sense.word} ${sense.hypernyms.headOption.getOrElse("")}"
  }

  private def readSenseFromFolder(folder: String): Sense = {
    val content = FileUtils.readContent(s"$folder/sense.json")
    Json.parse(content).as[Sense]
  }

  private val thumbnailName = "thumbnail.jpg"
  private val imageNotFoundName = "_IMAGE_NOT_FOUND"

  /**
    * Returns the path to the thumbnail if it exists, otherwise it checks for the _IMAGE_NOT_FOUND
    * file and returns None if it exists.
    * If both files do not exist, it throws a FileNotFoundException
    * @param sense
    * @return
    */
  def readThumbnailPath(sense: Sense): Option[String] = {
    val thumpnailPath = s"${bucketPath(sense)}/$thumbnailName"
    val notFoundPath = s"${bucketPath(sense)}/$imageNotFoundName"
    if (new File(thumpnailPath).exists())
      Some(thumpnailPath)
    else if (new File(notFoundPath).exists())
      None
    else
      throw new FileNotFoundException(s"Thumbnail image $thumpnailPath not found (also not found $notFoundPath file)!")
  }

  private def readPhotoURLFromFolder(folder: String) = {
    val content = FileUtils.readContent(s"$folder/response.json")
    val json = Json.parse(content)
    readThumbnailUrlFromJson(json)
  }

  private def readThumbnailUrlFromJson(json: JsValue) =
    (json \ "value").as[Seq[BingPhoto]].headOption.map(_.thumbnailUrl)

  private def readResult(folder: String): (String, Option[String]) = {
    val url = readPhotoURLFromFolder(folder)
    val sense = readSenseFromFolder(folder)
    (sense.uniqueID, url)
  }

  private def writeResultToDisk(sense: Sense,
                                apiRequest: WSRequest,
                                apiResponse: WSResponse,
                                optImageStream: Option[StreamedResponse])
                               (implicit s: ActorSystem, m: ActorMaterializer) = {
    val folder = bucketPath(sense)
    val future = Future {
      assert(!FileUtils.existsFile(folder), s"Folder for image download '$folder' already exists!")
      FileUtils.ensureFolderExists(folder)

      FileUtils.writeContent(s"$folder/query.uri", apiRequest.uri.toString)
      FileUtils.writeContent(s"$folder/sense.json", Json.toJson(sense).toString())
      FileUtils.writeContent(s"$folder/response.json", apiResponse.body)
    }.flatMap { _ =>
      optImageStream match {
        case Some(stream) =>
          // http://doc.akka.io/docs/akka/current/scala/stream/stages-overview.html#file-io-sinks-and-sources
          val fileSink = FileIO.toPath(new File(s"$folder/thumbnail.jpg").toPath)
          val inputStream = stream.body.runWith(fileSink)(m)
          inputStream

        case None =>
          Future { FileUtils.writeContent(s"$folder/$imageNotFoundName", "there has been no image in the response") }

      }
    }

    future
  }

  def readFromDiskOrDownload(sense: Sense)(implicit ws: WSClient, s: ActorSystem, m: ActorMaterializer): Future[Option[String]] =
    Future {
      readThumbnailPath(sense)
    }.recoverWith { case e: FileNotFoundException =>
      logger.info(s"Image for sense ${sense.uniqueID} not found on disk:\n" + e.getMessage)
      download(sense)
    }

  def download(sense: Sense)(implicit ws: WSClient, s: ActorSystem, m: ActorMaterializer): Future[Option[String]] = {

    val queryText = getQueryText(sense)
    val request = queryBingAPI(queryText)

    logger.info(s"Querying Bing API for images for '$queryText' (URI: ${request.uri})")

    request.get() flatMap { response =>
      response.status match {
        case Status.OK => Future.successful(response)
        case _ @ status => Future.failed(
          new Exception(s"Bing API response error: ${response.statusText} ($status)")
        )
      }
    } flatMap { response: WSResponse =>
      readThumbnailUrlFromJson(response.json) match {
        case Some(url) =>
          getImage(url) flatMap { stream =>
            stream.headers.status match {
              case Status.OK =>
                writeResultToDisk(sense, request, response, Some(stream))
              case _ @ status => Future.failed(
                new Exception(s"Downloading image from $url failed with response: ${response.statusText} ($status)")
              )
            }
          } map (_ => readThumbnailPath(sense))
        case None => Future {
          writeResultToDisk(sense, request, response, None)
          None
        }
      }
    }
  }

  private def getImage(url: String)(implicit ws: WSClient) = ws.url(url).withMethod("GET").stream()

  private def queryBingAPI(queryText: String)(implicit ws: WSClient) = {
    val query = Seq(
      "q" -> queryText,
      "count" -> "10",
      "offset" -> "0",
      "mkt" -> "en-us",
      "safeSearch" -> "Moderate",
      "safe_search" -> "1",
      "content_type" -> "1",
      "is_commons" -> "1",
      "format" -> "json",
      "nojsoncallback" -> "1"
    )
    val headers = Seq("Ocp-Apim-Subscription-Key" -> apiKey)
    val request = ws.url(apiEndpoint).withQueryString(query: _*).withHeaders(headers: _*)
    request
  }
}