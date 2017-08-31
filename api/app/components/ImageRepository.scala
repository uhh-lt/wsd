package components

import java.util.concurrent.ConcurrentHashMap
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.google.inject.ImplementedBy
import de.tudarmstadt.lt.wsd.common.model.Sense
import de.tudarmstadt.lt.wsd.bing_images.BingImageDownloader
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.WSClient

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by fide on 10.07.17.
  */

@ImplementedBy(classOf[ReadOrDownloadImageRepository])
trait ImageRepository {
  def get(sense: Sense): Future[Option[String]]
  def get(sense: Future[Sense]): Future[Option[String]]
}

@Singleton
class ReadOrDownloadImageRepository @Inject()(implicit lifecycle: ApplicationLifecycle,
                                              ws: WSClient,
                                              system: ActorSystem,
                                              downloader: BingImageDownloader) extends ImageRepository {

  private val cache = new ConcurrentHashMap[String, Future[Option[String]]]()

  import FunctionConverter._
  // TODO how to create materializer in Play?
  implicit val materializer = ActorMaterializer()

  lifecycle.addStopHook { () =>
    Future.successful(completeAllFutures())
  }

  override def get(sense: Sense): Future[Option[String]] = safeDownload(sense)
  override def get(sense: Future[Sense]): Future[Option[String]] = safeDownload(sense)

  private def safeDownload(sense: Sense): Future[Option[String]] =
    cache.computeIfAbsent(sense.uniqueID, (key: String) => downloader.readFromDiskOrDownload(sense))

  private def safeDownload(sense: Future[Sense]): Future[Option[String]] = sense.flatMap(safeDownload)

  private def completeAllFutures() = {
    import scala.collection.JavaConverters._
    val future = Future.sequence(cache.values().asScala)
    Await.result(future, 1 minute)
  }
}

// https://stackoverflow.com/a/36827674
object FunctionConverter {
  implicit def scalaFunctionToJava[From, To](function: (From) => To): java.util.function.Function[From, To] = {
    new java.util.function.Function[From, To] {
      override def apply(input: From): To = function(input)
    }
  }
}