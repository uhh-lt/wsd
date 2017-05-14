package de.tudarmstadt.lt.wsd.pipeline.utils
import org.apache.spark.sql.functions.udf
import de.tudarmstadt.lt.wsd.common.utils.JoBimTextTSVUtils._

/**
  * Created by fide on 01.12.16.
  */
object SparkJoBimTextUtils {

  def extrFeatureFromHolingUDF = udf(extractFeatureFromHoling)

  def splitClusterColumn = extrWordsWithWeights
  def splitIsasColumn = extrWordsWithWeights
  def splitHypernymColumn = extrWordsWithWeights
  def splitSenses = (col: String) => col.split(", ")

  def splitClusterUDF = udf(splitClusterColumn)
  def splitIsasUDF = udf(splitIsasColumn)
  def splitHypernymsUDF = udf(splitHypernymColumn)


  def splitSensesUDF = udf(splitSenses)
  def splitAndThenJoinWithoutSpaceAndWithWeight = (col: String) =>
    col.split(", ").map(x => s"$x:1.0").mkString(",")
  def appendIDByItself = (col: String) => s"$col#$col"
  def cosetSensesToClusterFormatUDF = udf(splitAndThenJoinWithoutSpaceAndWithWeight)
  def cosetIDToSenseIDFormatUDF = udf(appendIDByItself)


  def extrWordFromSenseIDUDF = udf(extractWordFromSenseID)

}
