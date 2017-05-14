package de.tudarmstadt.lt.wsd

import de.tudarmstadt.lt.wsd.pipeline.utils.RuntimeContext
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import de.tudarmstadt.lt.wsd.pipeline.feature.WeightedStringGroupingVectorizer

class WeightedStringGroupingVectorizerSpec extends FlatSpec with RuntimeContext {
  import spark.implicits._

  "Vectorizer" should "index correctly" in {
    val df = Seq(
      Tuple3("w2", 2, 2.0), Tuple3("w2", 1, 2.0),
      Tuple3("w1", 1, 1.0), Tuple3("w1", 3, 2.0)
    ).toDF("word", "featureIdx", "weight")

    val vectorizer = new WeightedStringGroupingVectorizer()
      .setGroupByCol("word")
      .setFeatureIdxCol("featureIdx")
      .setWeightCol("weight")
      .setNormalizeVector(true)
      .fit(df)

    val result = vectorizer.transform(df)
    // TODO check result

    //vectorizer.labels should equal (Array("f1", "f2", "f3")) // f1 has to be first,
    // because it is most freq
  }

  // TODO reproduce bug in a0328cdd584f9d2b923fdb7993e537a474d15380
}

