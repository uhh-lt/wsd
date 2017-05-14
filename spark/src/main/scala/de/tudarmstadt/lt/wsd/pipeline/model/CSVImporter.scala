package de.tudarmstadt.lt.wsd.pipeline.model

import de.tudarmstadt.lt.wsd.common.model.{Sense, WeightedWord}
import de.tudarmstadt.lt.wsd.common.utils.JoBimTextTSVUtils
import de.tudarmstadt.lt.wsd.common.utils.JoBimTextTSVUtils._
import de.tudarmstadt.lt.wsd.pipeline.utils.RuntimeContext
import de.tudarmstadt.lt.wsd.pipeline.utils.SparkJoBimTextUtils._
import de.tudarmstadt.lt.wsd.pipeline.utils.SparkUtils._
import org.apache.spark.ml.feature.StopWordsRemover
import org.apache.spark.sql.{DataFrame, Dataset, Row}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}

/**
  * Created by fide on 07.04.17.
  */
object CSVImporter extends RuntimeContext {

  import spark.implicits._
  private val parseWeightedArrayUDF = udf((x: String) => extrWordsWithWeights(x).map{case (s,d) => WeightedWord(s, d)})

  // Array[(String, Double)] from extrWordsWithWeights, seems not to be converted
  // to Seq[Row], http://stackoverflow.com/a/39006468
  private val removeWeightUDF = udf((arr: Seq[Row]) => arr.map(getFirstAs[String]))

  def loadTraditionalSenseClusters(path: String): DataFrame = {

    val df = sparkReadTSV(spark)
      .option("header", value=true)
      .load(path)
      .withColumn("sense_id", concat('word, lit("#"), 'cid))
      .withColumn("weighted_cluster_words", parseWeightedArrayUDF('cluster))
      .withColumn("cluster_words", removeWeightUDF('weighted_cluster_words))
      .withColumn("weighted_hypernyms", parseWeightedArrayUDF('isas))
      .withColumn("hypernyms", removeWeightUDF('weighted_hypernyms))
      .select("sense_id", "word", "weighted_cluster_words", "cluster_words",
        "weighted_hypernyms", "hypernyms")

    df
  }

  def loadCoSetSenseClusters(path: String): DataFrame = {
    val parseSensesUDF = udf(extrWordsFromSeses _)
    val addNormWeightToArray = udf((arr: Seq[String]) => arr.map(WeightedWord(_, 1.0)))

    val df = sparkReadTSV(spark)
      .option("header", value=true)
      .load(path)
      .withColumn("sense_id", 'cid)
      .withColumn("cluster_words", parseSensesUDF('senses))
      .withColumn("weighted_cluster_words", addNormWeightToArray('cluster_words))
      .withColumn("weighted_hypernyms", parseWeightedArrayUDF('hypernyms))
      .withColumn("hypernyms", removeWeightUDF('weighted_hypernyms))
      .withColumn("word", lit(""))
      .select("sense_id", "word", "weighted_cluster_words", "cluster_words",
        "weighted_hypernyms", "hypernyms")
    df
  }

  def loadContextFeatures(path: String): Dataset[ContextFeature] = {
    val schema = StructType(Array(
      StructField("word", StringType, nullable = false),
      StructField("holing", StringType, nullable = false),
      StructField("weight", DoubleType, nullable = false)
    ))

    val extrHeadFromDep = udf(JoBimTextTSVUtils.extractFeatureFromHoling)

    val df = sparkReadTSV(spark)
      .option("header", value = false)
      .schema(schema)
      .load(path)
      .withColumn("head", extrHeadFromDep('holing)) // technically this is head or end of dep
      .filter('head =!= "") // Remove head extraction failures

    df.as[ContextFeature]
  }
}
