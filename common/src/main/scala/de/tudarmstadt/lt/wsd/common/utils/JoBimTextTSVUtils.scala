package de.tudarmstadt.lt.wsd.common.utils

import StringUtils._
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.util.matching.Regex

/**
  * Created by fide on 02.12.16.
  */
object JoBimTextTSVUtils extends LazyLogging {
  // "bla foo#123" => "bla foo"
  def extrWordsFromSenseID(id: String): String = id.split("#").headOption.getOrElse("")

  def extrWordsFromSeses(senses: String): Array[String] = {
    senses.split(",").map(_.trim).map(extrWordsFromSenseID)
  }

  // "bla#123:0.5,foo#3:22.3" => Array(("bla", 0.5), ("foo", 22.3))
  def extrWordsWithWeights: (String) => Array[(String, Double)] = { rawStr: String =>
    if (rawStr == null) {
      Array.empty[(String, Double)]
    } else if (rawStr.trim == "") {
      Array.empty[(String, Double)]
    } else {
      rawStr.trim.split(",").flatMap(s =>
        s.split(":") match {
          case Array(a, b) => Some(extrWordsFromSenseID(a), b.toDouble)
          case x@_ =>
            logger.debug(s"extrWordsWithWeights could not extract, splitted was this: '${x.mkString("', '")}'")
            None
        })
    }
  }

  /** Converts a holing operation as it is used in LEFEX (.ie. only with one @) into the dependent feature, for
    * general information see: http://maggie.lt.informatik.tu-darmstadt.de/jobimtext/documentation/the-holing-operation/
    * For the specific syntax of the here expected represeantion see: https://github.com/tudarmstadt-lt/lefex
    *
    *  {{{
    *  scala> val holing = "prep_of(great,@)"
    *  scala> extractFeatureFromHoling(holing)
    *  res0: String = "great"
    *  }}}
    */
  def extractFeatureFromHoling = { holing: String =>
    val pattern = """(?<=\((@,)?)[^,@]*(?=(,@)?\))""".r
    pattern.findFirstIn(holing) match {
      case Some(feat) => feat
      case None =>
        logger.debug(s"Could not extract feature from holing: $holing")
        ""
    }
  }

  def saveExtractFeatureFromHoling = { holing: String =>
    holing match {
      case x if x.contains("@,") =>
        val pattern = """(\w+)\(@,(.+)\)""".r
        val pattern(op, feat) = x
        assert(s"$op(@,$feat)" == x)
        feat
      case x if x.contains(",@") =>
        val pattern = """(\w+)\((.+),@\)""".r
        val pattern(op, feat) = x
        assert(s"$op($feat,@)" == x)
        feat
      case _ =>
        logger.debug(s"Could not extract feature from holing: $holing")
        ""
    }
  }

  def extractWordFromSenseID = { senseID: String =>
    val pattern = """.*(?=#\d+$)""".r
    pattern.findFirstIn(senseID).getOrElse("")
  }
}
