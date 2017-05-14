package de.tudarmstadt.lt.wsd

import de.tudarmstadt.lt.wsd.pipeline.feature.WeightedOneHotEncoder
import de.tudarmstadt.lt.wsd.pipeline.utils.RuntimeContext
import org.apache.spark.sql.types.MetadataBuilder
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class WeightedOneHotEncoderSpec extends FlatSpec with RuntimeContext {
  import spark.implicits._

  "Encoder" should "not only store num_attrs as metadata for sparse vectors" in {
    val df = (1 to 2).map(Tuple2(_, 0.1)).toDF("idx", "weight")

    val encoder = new WeightedOneHotEncoder()
      .setInputCol("idx")
      .setWeightCol("weight")
      .setOutputCol("vector")

    val encoded = encoder.transform(df)
    val field = encoded.schema("vector")

    val expectedMetadata = new MetadataBuilder()
      .putMetadata(
        "ml_attr",
        new MetadataBuilder().putDouble("num_attrs", 3).build())
      .build()

    field.metadata should equal(expectedMetadata)
  }
}

