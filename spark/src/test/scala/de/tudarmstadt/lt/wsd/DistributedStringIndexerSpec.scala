package de.tudarmstadt.lt.wsd

import de.tudarmstadt.lt.wsd.pipeline.utils.RuntimeContext
import org.apache.spark.ml.attribute.AttributeGroup
import org.apache.spark.ml.feature.DistributedStringIndexer
import org.apache.spark.sql.Row
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class DistributedStringIndexerSpec extends FlatSpec with RuntimeContext {
  import spark.implicits._

  "Word" should "get indexed" in {
    val df = Seq(
      Some("hello"),
      Some("world")
    ).toDF("word")

    val indexer = new DistributedStringIndexer()
      .setInputCol("word")
      .setOutputCol("word_idx")
      .fit(df)

    val result = indexer.transform(df).collect()

    result should equal (Array(Row("hello", 0.0), Row("world", 1.0)))
  }

  "Frequency" should "correspond to index position" in {

    val df = Seq(
      Some("hello"),
      Some("world"),
      Some("world")
    ).toDF("word")

    val indexer = new DistributedStringIndexer()
      .setInputCol("word")
      .setOutputCol("word_idx")
      .fit(df)

    val result = indexer.transform(df).collect()

    result should equal (Array(Row("world", 0.0), Row("world", 0.0), Row("hello", 1.0)))
  }


}

