package de.tudarmstadt.lt.wsd.common.model.images

import javax.inject.{Inject, Singleton}

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
  * Created by fide on 24.03.17.
  */

@Singleton
class FlickrDownloader @Inject()(ws: WSClient) extends LazyLogging {
  import play.api.libs.concurrent.Execution.Implicits._

  implicit val photoReads = Json.reads[FlickrPhoto]
  val config = ConfigFactory.load()
  val apiKey = config.getString("wsd.common.flickr.api_key")

  val apiEndpoint = "https://api.flickr.com/services/rest"

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
