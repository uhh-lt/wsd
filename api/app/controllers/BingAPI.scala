package controllers

import java.net.{URI, URLDecoder}
import java.security.MessageDigest
import javax.inject.{Inject, Singleton}

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import de.tudarmstadt.lt.wsd.common.model.{Sense, SenseInventory, SenseVectorModel}
import de.tudarmstadt.lt.wsd.common.utils.FileUtils
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws._
import play.api.mvc.{Action, Controller}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
chmod a+w tmp/bing_images
  */
@Singleton
class BingAPI @Inject() (ws: WSClient) extends Controller  with LazyLogging {

  import Implicits._

  implicit val photoReads = Json.reads[BingPhoto]
  val config = ConfigFactory.load()
  val apiKey = config.getString("wsd.api.bing.api_key")
  val imageFolder = config.getString("wsd.api.bing.image_folder")

  val apiEndpoint = "https://api.cognitive.microsoft.com/bing/v7.0/images/search"

  def redirectToPhoto(inventory: String, encoded_sense_id: String) = Action {
     try {
       val enum = Sense withName inventory
       val sense_id = URLDecoder.decode(encoded_sense_id, "UTF-8")
       Sense.findByInventoryAndId(enum, sense_id) match {
         case Some(sense) =>
           val optURL = getPhotoURL(sense)
           optURL match {
             case Some(url) =>  Redirect(url)
             case None => NotFound("No images found.")
           }
         case None => NotFound(s"No sense found for: sense_id: $sense_id, inventory: $inventory.")
       }
    } catch {
      case e: NoSuchElementException => NotFound(s"Invalid inventory name: $inventory")
    }
  }

  def md5(s: String): String = {
    MessageDigest.getInstance("MD5").digest(s.getBytes).map("%02X" format _).mkString
  }


  def getPhotoURL(sense: Sense): Option[String] = {

    val queryText = {
      if (sense.isInventoryCoset)
        s"${sense.hypernyms.take(3).mkString(" ")}"
      else
        s"${sense.word} ${sense.hypernyms.headOption.getOrElse("")}"
    }

    val senseIdMd5 = md5(sense.sense_id)
    val senseImageFolder = s"$imageFolder/${senseIdMd5.substring(2)}/$senseIdMd5"
    val responseFile = s"$senseImageFolder/response.json"
    val senseFile = s"$senseImageFolder/sense.json"

    val json = if (FileUtils.existsFile(responseFile)) {
      val content = FileUtils.readContent(responseFile)
      Json.parse(content)
    } else {
      val response = queryBingImageAPI(queryText)
      FileUtils.ensureFolderExists(senseImageFolder)
      FileUtils.writeContent(senseFile, Json.toJson(sense).toString())
      FileUtils.writeContent(responseFile, response.body)
      response.json
    }
    (json \ "value").as[Seq[BingPhoto]].headOption.map(_.thumbnailUrl)
  }

  def queryBingImageAPI(queryText: String): WSResponse = {
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
    logger.info(s"Search Images from Bing API for '$queryText' at URI: '${request.uri}'")
    Await.result(request.get(), 2.second)
  }

}
case class BingPhoto(thumbnailUrl: String)
