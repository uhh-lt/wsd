package de.tudarmstadt.lt.wsd.bing_images

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import de.tudarmstadt.lt.wsd.common.model.{Sense, WeightedWord}
import de.tudarmstadt.lt.wsd.common.utils.FileUtils
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, FlatSpec}
import play.api.libs.json._
import play.api.mvc.{Action, Results}
import play.api.test.WsTestClient
import play.core.server.Server
import play.api.routing.sird._

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by fide on 21.06.17.
  */
class BingDownloaderSpec extends FlatSpec with BeforeAndAfterEach with ScalaFutures {

  private val config = ConfigFactory.load()

  override implicit val patienceConfig = PatienceConfig(
    timeout = scaled(20 second),
    interval = scaled(100 millis)
  )

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val sense: Sense = Sense(sense_id="testcase#1",
    inventory="traditional",
    word="testcase",
    hypernyms=Seq("hyp1"),
    weighted_hypernyms=Seq(WeightedWord("hyp1", 0.1)),
    cluster_words=Seq("word1"),
    weighted_cluster_words=Seq(WeightedWord("word1", 0.2))
  )

  "bing_images" should "provide an image path" in {
    Server.withRouter() {
      case GET(p"/bing/v5.0/images/search") => Action {
        Results.Ok(Json.obj("value" -> Json.arr(Json.obj("thumbnailUrl" -> "/test.jpg"))))
      }
      case GET(p"/test.txt") => Action {
        Results.Ok("test ok")
      }
      case GET(p"/test.jpg") => Action {
        val path = getClass.getResource("/test.jpg").getPath
        Results.Ok.sendFile(new java.io.File(path))
        //Results.Ok.sendResource("test.jpg", inline = false)
        //Results.Ok.sendFile(new File("test.jpg"))
      }
    } { implicit port =>
      WsTestClient.withClient { client =>
        val downloader = new BingImageDownloader("")

        val pathFuture = downloader.download(sense)(client, system, materializer)

        whenReady(pathFuture) { path =>
          assert(path.exists(_.endsWith("/thumbnail.jpg")))
        }

        val cache = downloader.readCache
        assert(cache === Map("testcase#1-traditional" -> Some("/test.jpg")))
      }
    }
  }

  def deleteImageFolder(): Unit = {
    val imageFolder = config.getString("wsd.bing_images.image_folder")
    FileUtils.deleteFolderIfExists(imageFolder)
  }

  override def beforeEach: Unit = deleteImageFolder()
  //override def afterEach: Unit = deleteImageFolder()
}
