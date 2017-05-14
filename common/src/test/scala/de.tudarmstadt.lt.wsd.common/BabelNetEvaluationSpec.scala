package de.tudarmstadt.lt.wsd.common

import de.tudarmstadt.lt.wsd.common.utils.Utils
import org.scalatest.FlatSpec

class BabelNetEvaluationSpec extends FlatSpec {

  "BabelnetCorpus" should "read" in {
    val corpusLocation = Utils.getResourcePath("/")
    val outputLocation = "/tmp/results-" + Utils.generateRandomName()
    val words = Seq("tall")
  }
}

