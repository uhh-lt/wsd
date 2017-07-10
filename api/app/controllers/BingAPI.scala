package controllers

import java.io.File
import java.net.URLDecoder
import javax.inject.{Inject, Singleton}

import components.ImageRepository
import de.tudarmstadt.lt.wsd.common.model.Sense
import play.api.mvc.{Action, AnyContent, Controller}

import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * chmod a+w imgdata/bing
  */
@Singleton
class BingAPI @Inject() (repository: ImageRepository) extends Controller {

  def photo(inventoryStr: String, encoded_sense_id: String): Action[AnyContent] = Action.async {
    val inventory = Try(Sense withName inventoryStr)
    val senseID = Try(URLDecoder.decode(encoded_sense_id, "UTF-8"))
    val sense = senseID.flatMap(id => inventory.map(x => Sense.findByInventoryAndId(x, id).get))
    val url = Future.fromTry(sense).flatMap(repository.get)
    url.map(x => Ok.sendFile(new File(x)))
  }

}
