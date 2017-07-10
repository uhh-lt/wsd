package de.tudarmstadt.lt.wsd.common.model.images

/**
  * Created by fide on 20.06.17.
  */
case class FlickrPhoto(id: String, owner: String, secret: String, server: String, farm: Int, title: String) {
  val url = s"https://farm$farm.staticflickr.com/$server/${id}_${secret}_m.jpg"
}
