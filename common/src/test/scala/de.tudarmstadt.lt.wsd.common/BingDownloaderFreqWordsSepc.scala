package de.tudarmstadt.lt.wsd.common

import java.io.File

import com.typesafe.config.ConfigFactory
import de.tudarmstadt.lt.wsd.common.model.Sense.InventoryName
import de.tudarmstadt.lt.wsd.common.model.images.BingImageDownloader
import de.tudarmstadt.lt.wsd.common.model.{Sense, WeightedWord}
import de.tudarmstadt.lt.wsd.common.utils.FileUtils
import org.apache.commons.io.{FileUtils => IOFileUtils}
import org.scalatest.{BeforeAndAfterEach, fixture, _}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Results}
import play.api.routing.sird._
import play.api.test.WsTestClient
import play.core.server.Server
import scalikejdbc._
import scalikejdbc.scalatest._
import skinny._

import scala.collection.Map

//https://github.com/skinny-framework/skinny-framework/blob/master/skinny-blank-app/build.sbt
//http://scalikejdbc.org/documentation/auto-session.html

@Ignore class BingDownloaderFreqWordsSepc extends fixture.FunSpec with AutoRollback with Matchers with DBSettings with BeforeAndAfterEach {
  val wordFreqs = Seq(
    Map("word" -> "rare", "freq" -> "1"),
    Map("word" -> "test", "freq" -> "100"),
    Map("word" -> "word", "freq" -> "1")
  )

  override def fixture(implicit session: DBSession) {
    // Do not forget to prepare the development DB, with something similar to the following:
    // sed -i '/  db:/a\    ports: [ "5432:5432" ] ' docker-compose.override.yml
    // docker-compose up -d db
    // docker-compose exec db createdb -U postgres -T wsp_default wsp_development_default
    //

    for (word <- wordFreqs.map(_("word")); id <- 1 to 3) yield
      insertSenseIntoDB(word, s"$word#$id", Sense.traditional)
  }

  private def insertSenseIntoDB(word: String, sense_id: String, inventory: InventoryName)(implicit session: DBSession) =
    Sense.createWithAttributes(
      'word -> word,
      'sense_id -> sense_id,
      'inventory -> inventory.toString,
      'cluster_words -> "",
      'weighted_cluster_words -> "",
      'hypernyms -> "hyp1",
      'weighted_hypernyms -> "hyp1:0.1"
    )

  private def withBingAPIMock(block: WSClient => Unit) = {
    var counter = 0
    Server.withRouter() {
      case GET(p"/bing/v5.0/images/search") => Action {
        counter = counter + 1
        if (counter < 5)
          Results.Ok(Json.obj("value" -> Json.arr(Json.obj("thumbnailUrl" -> "/test.jpg"))))
        else
          Results.Forbidden("Your request limit has been exceeded!")

      }
      case GET(p"/test.jpg") => Action {
        Results.Ok.sendFile(new java.io.File("/home/fide/Workspace/wsd/api/test.jpg"))
      }
    } { implicit port =>
      WsTestClient.withClient(block)
    }
  }

  describe("BingDownload") {
    it("should download first five senses by frequency") { implicit session =>
      var counter = 0
      Server.withRouter() {
        case GET(p"/bing/v5.0/images/search") => Action {
          counter = counter + 1
          if (counter < 5)
            Results.Ok(Json.obj("value" -> Json.arr(Json.obj("thumbnailUrl" -> "/test.jpg"))))
          else
            Results.Forbidden("Your request limit has been exceeded!")

        }
        case GET(p"/test.txt") => Action {
          Results.Ok("test ok")
        }
        case GET(p"/test.jpg") => Action {
          Results.Ok.sendFile(new java.io.File("/home/fide/Workspace/wsd/api/test.jpg"))
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val downloader = new BingImageDownloader("")
          //downloader.downloadMostFreq(wordFreqs, 5)(session)

          val imageFolder = config.getString("wsd.common.bing.image_folder")
          assert(counter == 5)
        }
      }
    }
  }

  private val config = ConfigFactory.load()

  def deleteImageFolder(): Unit = {
    val imageFolder = config.getString("wsd.common.bing.image_folder")
    FileUtils.deleteFolderIfExists(imageFolder)
  }

  override def beforeEach: Unit = deleteImageFolder()
  //override def afterEach: Unit = deleteImageFolder()
}
