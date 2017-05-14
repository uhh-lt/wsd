package de.tudarmstadt.lt.wsd.pipeline.utils

import com.twitter.chill.KryoInjection
import org.apache.spark.ml.linalg.{LinalgShim, Vector}
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.functions._

/**
  * Created by fide on 01.12.16.
  */
object SparkUtils {

  def serializeVector = {
    vec: Vector => KryoInjection(LinalgShim.asBreeze(vec))
  }

  def serializeVecUDF = udf(serializeVector)

  def getFirstAs[T] = { e: Row => e.getAs[T](0) }
  def getSecondAs[T] = { e: Row => e.getAs[T](1) }

  def getFirstAsStringUDF = udf(getFirstAs[String])
  def getSecondAsDoubleUDF = udf(getSecondAs[Double])

  def scaleVectorUDF = udf(LinalgShim.scale _)

  def addVectorsUDF = udf(LinalgShim.add _)

  def normalizeVectorUDF = udf(LinalgShim.normalize _)
  
  val sparkReadTVSWithWeiredCharAsQuote = { spark: SparkSession =>
    spark.read
      .format("csv")
      .option("delimiter", "\t")
      .option("quote", "元") // TODO
      .option("mode", "DROPMALFORMED")
  }
  
  val sparkReadTSV = sparkReadTVSWithWeiredCharAsQuote
}
