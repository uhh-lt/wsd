package de.tudarmstadt.lt.wsd.common

import de.tudarmstadt.lt.wsd.common.utils.{JoBimTextTSVUtils, ScalaNLPUtils, StringUtils, Utils}

import scala.collection.JavaConversions._

trait FeatureExtractor {
  def extractFeatures(context: String, word: String): List[String]
}

object WordFeatureExtractor extends FeatureExtractor {
  def extractFeatures(context: String, word: String): List[String] = {
    context.split(" ").filter(_.toLowerCase != word.toLowerCase()).toList
  }
}

object LemmaFeatureExtractor extends FeatureExtractor {
  def extractFeatures(context: String, word: String): List[String] = {
    val cleanedContext = context.toLowerCase
    ScalaNLPUtils.convertToLemmas(cleanedContext).filter(_ != word.toLowerCase)
  }
}

object LemmaPlusWordFeatureExtractor extends FeatureExtractor {
  def extractFeatures(context: String, word: String): List[String] = {
    val lemmas = LemmaFeatureExtractor.extractFeatures(context, word)
    val words = WordFeatureExtractor.extractFeatures(context, word)
    (lemmas ::: words).distinct
  }
}

object OnlyDepFromHolingExtractor extends FeatureExtractor {
  def extractFeatures(context: String, word: String): List[String] = {
    context.split("  ")
      .filter(_.nonEmpty)
      .map{JoBimTextTSVUtils.extractFeatureFromHoling}
      .toList
  }
}

