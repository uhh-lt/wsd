package controllers

import javax.inject.{Inject, Singleton}

import de.tudarmstadt.lt.wsd.common.model.images.FlickrDownloader
import play.api.mvc.{Action, Controller}

/**
  * Created by fide on 24.03.17.
  */

@Singleton
class FlickrAPI  @Inject()(downloader: FlickrDownloader) extends Controller {

  def redirectToPhoto(text: String) = Action {
    val optURL = downloader.getPhotoURL(text)
    optURL match {
      case Some(url) => Redirect(url)
      case None => Ok("Not found")
    }
  }
}
