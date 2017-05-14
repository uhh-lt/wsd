package controllers

import javax.inject.{Inject, Singleton}

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws._
import play.api.mvc.{Action, Controller}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by fide on 24.03.17.
  */

@Singleton
class FlickrAPI @Inject() (ws: WSClient) extends Controller  with LazyLogging {

  implicit val photoReads = Json.reads[FlickrPhoto]
  val config = ConfigFactory.load()
  val apiKey = config.getString("wsd.api.flickr.api_key")

  val apiEndpoint = "https://api.flickr.com/services/rest"

  def redirectToPhoto(text: String) = Action {
    val optURL = getPhotoURL(text)
    optURL match {
      case Some(url) =>  Redirect(url)
      case None => Ok("Not found")
    }
  }

  def getPhotoURL(text: String): Option[String] = {
    val query = Seq(
      "method" -> "flickr.photos.search",
      "text" -> text,
      "api_key" -> apiKey,
      "tags" -> "",
      "safe_search" -> "1",
      "content_type" -> "1",
      "is_commons" -> "1",
      "format" -> "json",
      "nojsoncallback" -> "1"
    )

    val request = ws.url(apiEndpoint).withQueryString(query: _*)

    logger.info(s"Search Images from Flickr API for '$text' at URI: '${request.uri}'")

    val futureResult: Future[Seq[FlickrPhoto]] = request.get.map {
      response =>
        (response.json \ "photos" \ "photo").as[Seq[FlickrPhoto]]
    }

    Await.result(futureResult, 2.second).headOption.map(_.url)
  }

}
case class FlickrPhoto(id: String, owner: String, secret: String, server: String, farm: Int, title: String) {
  val url = s"https://farm$farm.staticflickr.com/$server/${id}_${secret}_m.jpg"
}
