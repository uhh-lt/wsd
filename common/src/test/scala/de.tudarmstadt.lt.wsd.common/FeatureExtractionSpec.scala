package de.tudarmstadt.lt.wsd.common

import de.tudarmstadt.lt.wsd.common.utils.NLPUtils
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class FeatureExtractionSpec extends FlatSpec {

  "Lemmas" should "be extracted from sentence" in {
    val sentence = "Tables with chair."
    val lemmas = NLPUtils.convertToLemmas(sentence)

    lemmas should contain ("table")
  }

  "Dependency feautre" should "be extracted correctly" in {
    val context = "Tables with chair."
    val word = "table"
    val features = DependencyFeatureExtractor.extractFeatures(context, word)

    features should contain ("chair#NN#prep_with")
    features should not contain "ROOT#null#-root"
  }
}

