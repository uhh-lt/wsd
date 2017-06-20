package de.tudarmstadt.lt.wsd.common

import de.tudarmstadt.lt.wsd.common.prediction.{DetailedPredictionPipeline, WSDModel}
import de.tudarmstadt.lt.wsd.common.utils.Utils
import org.scalatest.FlatSpec
import scalikejdbc.config.DBs

class BabelNetEvaluationSpec extends FlatSpec {
  DBs.setupAll()

  "BabelnetCorpus" should "read" in {
    val corpusLocation = Utils.getResourcePath("/")
    val outputLocation = "/tmp/results-" + Utils.generateRandomName()
    val words = Seq("tall")
  }

  "Test" should "test" in {
    val model = WSDModel.parseFromString("naivebayes_traditional_self")
    val pipeline = DetailedPredictionPipeline.get(model)
    pipeline.predictWordInContext("jaguar", "The Jaguar is a superhero published by Archie Comics.")
  }
}

