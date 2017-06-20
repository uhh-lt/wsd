package de.tudarmstadt.lt.wsd.common

import de.tudarmstadt.lt.wsd.common.utils.{JoBimTextTSVUtils, NLPUtils, StringUtils, Utils}
import edu.stanford.nlp.ling.Sentence
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import edu.stanford.nlp.trees.PennTreebankLanguagePack

import scala.collection.JavaConversions._

trait FeatureExtractor {
  def extractFeatures(context: String, word: String): List[String]
}

object DependencyFeatureExtractor extends FeatureExtractor {
  // Usage inspired by:
  // http://nlp.stanford.edu/nlp/javadoc/javanlp-3.3.1/edu/stanford/nlp/parser/lexparser/package-summary.html
  val modelPath = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz"
  val lexParser = LexicalizedParser.loadModel(modelPath)

  def extractFeatures(context: String, word: String): List[String] = {
    // TODO: actually a word with POS would be needed here
    val lemmas = NLPUtils.convertToLemmas(context)

    val rawWords = Sentence.toCoreLabelList(lemmas: _*)
    val parse = lexParser.apply(rawWords)
    val tlp = new PennTreebankLanguagePack()
    val gsf = tlp.grammaticalStructureFactory()
    val gs = gsf.newGrammaticalStructure(parse)
    val tdl = gs.typedDependenciesCCprocessed()

    tdl.toList.flatMap{dep =>
      val reln = dep.reln()

      if (dep.dep().value() == "ROOT" || dep.gov().value() == "ROOT") {
        None
      } else if (word == dep.gov().value().toLowerCase) {
        val depToken = s"${dep.dep().value()}#${dep.dep().label().tag()}"
        Some(s"$depToken#$reln")
      } else if (word == dep.dep().value().toLowerCase) {
        val govToken = s"${dep.gov().value()}#${dep.gov().label().tag()}"
        Some(s"$govToken#-$reln")
      } else {
        None
      }
    }
  }
}

object WordFeatureExtractor extends FeatureExtractor {
  def extractFeatures(context: String, word: String): List[String] = {
    context.split(" ").filter(_.toLowerCase != word.toLowerCase()).toList
  }
}

object LemmaFeatureExtractor extends FeatureExtractor {
  def extractFeatures(context: String, word: String): List[String] = {
    val cleanedContext = context.toLowerCase
    NLPUtils.convertToLemmas(cleanedContext).filter(_ != word.toLowerCase)
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

