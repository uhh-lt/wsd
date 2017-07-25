package de.tudarmstadt.lt.wsd.common

import de.tudarmstadt.lt.wsd.common.utils.ScalaNLPUtils
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class ScalaNLPUtilsSpec extends FlatSpec {

  "Lemmas" should "be extracted from sentence" in {
    val sentence = "Tables with chair."
    val lemmas = ScalaNLPUtils.convertToLemmas(sentence)

    lemmas should contain ("table")
  }
}

