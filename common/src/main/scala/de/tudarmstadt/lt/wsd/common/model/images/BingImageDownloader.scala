package de.tudarmstadt.lt.wsd.common.model.images

import java.io.{File, FileNotFoundException}
import java.security.MessageDigest

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
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
  private val apiKey = config.getString("wsd.common.bing.api_key")
  private val imageFolder = config.getString("wsd.common.bing.image_folder")
  private val apiEndpoint = baseUrl + "/bing/v5.0/images/search"

  def readCache: Map[String, String] = {
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

  def thumbnailPath(sense: Sense): String = s"${bucketPath(sense)}/thumbnail.jpg"
  def doesThumbnailExist(sense: Sense): Boolean = new File(thumbnailPath(sense)).exists()

  @deprecated
  def readPhotoURLFromFolder(folder: String): String = {
    logger.debug(s"Read image url from $folder")
    val content = FileUtils.readContent(s"$folder/response.json")
    val json = Json.parse(content)
    readThumbnailUrlFromJson(json)
  }

  @deprecated
  def readPhotoURLFromFolder(sense: Sense): String = readPhotoURLFromFolder(bucketPath(sense))

  private def readThumbnailUrlFromJson(json: JsValue) =
    (json \ "value").as[Seq[BingPhoto]].headOption.map(_.thumbnailUrl).get
  
  private def readResult(folder: String): (String, String) = {
    val url = readPhotoURLFromFolder(folder)
    val sense = readSenseFromFolder(folder)
    (sense.uniqueID, url)
  }

  private def writeResultToDisk(sense: Sense,
                                       apiRequest: WSRequest,
                                       apiResponse: WSResponse,
                                       imageStream: StreamedResponse)
                                      (implicit s: ActorSystem, m: ActorMaterializer) = {
    val folder = bucketPath(sense)
    val future = Future {
      assert(!FileUtils.existsFile(folder), s"Folder for image download '$folder' already exists!")
      FileUtils.ensureFolderExists(folder)

      FileUtils.writeContent(s"$folder/query.uri", apiRequest.uri.toString)
      FileUtils.writeContent(s"$folder/sense.json", Json.toJson(sense).toString())
      FileUtils.writeContent(s"$folder/response.json", apiResponse.body)
    }.flatMap { _ =>
      // http://doc.akka.io/docs/akka/current/scala/stream/stages-overview.html#file-io-sinks-and-sources
      val fileSink = FileIO.toPath(new File(s"$folder/thumbnail.jpg").toPath)

      val inputStream = imageStream.body.runWith(fileSink)(m)
      inputStream
    }

    future
  }

  def readFromDiskOrDownload(sense: Sense)(implicit ws: WSClient, s: ActorSystem, m: ActorMaterializer): Future[String] =
    Future {
      if (!doesThumbnailExist(sense))
        throw new FileNotFoundException(s"Thumbnail image not found: ${thumbnailPath(sense)}")
      thumbnailPath(sense)
    }.recoverWith{case e: FileNotFoundException =>
      logger.debug(s"Image for sense ${sense.uniqueID} not found on disk.")
      download(sense)
    }

  def download(sense: Sense)(implicit ws: WSClient, s: ActorSystem, m: ActorMaterializer): Future[String] = {

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
      val url = readThumbnailUrlFromJson(response.json)
      getImage(url) flatMap { stream =>
        stream.headers.status match {
          case Status.OK =>
            writeResultToDisk(sense, request, response, stream)
          case _ @ status => Future.failed(
            new Exception(s"Downloading image from $url failed with response: ${response.statusText} ($status)")
          )
        }
      } map (_ => thumbnailPath(sense))
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