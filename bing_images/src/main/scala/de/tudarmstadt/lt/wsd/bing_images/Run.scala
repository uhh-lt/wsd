package de.tudarmstadt.lt.wsd.bing_images

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import de.tudarmstadt.lt.wsd.common.model.Sense
import de.tudarmstadt.lt.wsd.common.utils.TSVUtils
import play.api.libs.ws.ahc.AhcWSClient
import scalikejdbc.config._

import scala.concurrent.Await
import scala.util.{Failure, Success}

/**
  * Created by fide on 07.12.16.
  */


object Run extends LazyLogging {

  DBs.setupAll()

  object Command extends Enumeration {
    type Name = Value
    val downloadimages, nocmd = Value
  }

  import Command._

  case class Params(mode: Command.Name = Command.nocmd,
                    frequencyFile: String = "",
                    downloadLimit: Option[Int] = None
                   )

  val config = ConfigFactory.load()

  implicit val commandsRead: scopt.Read[Command.Value] =
    scopt.Read.reads(Command withName)

  val parser = new scopt.OptionParser[Params]("wsd-bing-images") {
    head("wsp-bing-images", "0.3.x")

    cmd("downloadimages").action((_, c) => c.copy(mode = downloadimages)).
      text("Download images for cache from Bing").
      children(
        opt[String]('f', "frequencyFile").required()
          .action((x,c) => c.copy(frequencyFile = x))
          .text("A tab seperated CSV file with Sense IDs and their frequencies as columns."),
        opt[Int]('l', "limit").required()
          .action((x,c) => c.copy(downloadLimit = Some(x)))
          .text("Number of maximal downloads")
      )

    checkConfig {
      case _ => success
    }
  }

  def main(args: Array[String]): Unit = {

    parser.parse(args, Params()) match {
      case Some(params) =>
        params.mode match {
          case `downloadimages` => mainDownloadImages(params.frequencyFile, params.downloadLimit)
        }
      case None =>
      // arguments are bad, error message will be displayed by scopt
    }
  }

  def mainDownloadImages(frequencyFile: String, maxOpt: Option[Int]): Unit = {
    val max = maxOpt.getOrElse(10)

    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val wsClient: AhcWSClient = AhcWSClient()

    val (_, wordFreqs) = TSVUtils.readWithHeaders(frequencyFile, removeIncomplete = true)

    val mostFreq = wordFreqs
      .sortBy(-_("freq").toInt)
      .map(_("word").toLowerCase)
      .distinct

    println(s"STARTING DOWNLOADS (all senses for maximal $max words)\n")

    val downloader = new BingImageDownloader()

    val senses = mostFreq.toStream.flatMap(word => Sense.findAllByCaseIgnoredWord(word))
    val processed = senses.map { sense =>

      println(s"Checking image for ${sense.uniqueID}.")

      val check = downloader.readFromDiskOrDownload(sense)

      import scala.concurrent.ExecutionContext.Implicits.global
      check.onComplete {
        case Success(url) => println(s"Successfully ensured image for ${sense.uniqueID} exists.")
        case Failure(exception) => println(s"Error for ${sense.uniqueID}: $exception\n")
      }

      check

    } take max takeWhile { check =>
      import scala.concurrent.duration._
      // https://stackoverflow.com/a/21417522
      Await.ready(check, 1 minutes).value.get.isSuccess
    } length

    println(s"$processed senses processed")

    wsClient.close()
    system.terminate()
  }
}
