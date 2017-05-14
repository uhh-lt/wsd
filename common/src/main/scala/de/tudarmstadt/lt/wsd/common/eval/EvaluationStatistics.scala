package de.tudarmstadt.lt.wsd.common.eval
import de.tudarmstadt.lt.wsd.common.utils.TSVUtils
import de.tudarmstadt.lt.wsd.common.prediction.DetailedPredictionPipeline._
/**
  * Created by fide on 23.01.17.
  */

object EvaluationStatistics {
  def create(dataSetPath: String): EvaluationStatistics = {
    val (_, rows) = TSVUtils.readWithHeaders(dataSetPath)
    EvaluationStatistics(rows.toList)
  }

  def calcPercentage(map: Map[String, Int]): Double = {
    (map.get("true"), map.get("false")) match {
      case (Some(t), Some(f)) => t.toFloat/(t + f)
      case (Some(t), None) => 1.0
      case (None, Some(f)) => 0.0
      case (None, None) => Float.NaN
    }
  }
}

case class EvaluationStatistics(predictions: List[Map[String, String]]) {
  val total = predictions.length
  val numPredicted = predictions.count(_("predict_sense_ids") != REJECT_OPTION_ID)
  val numCorrect = predictions.count(_("correct") == "true")
  val precision = EvaluationStatistics.calcPercentage(
    predictions.filter(_("predict_sense_ids") != REJECT_OPTION_ID).groupBy(_("correct")).mapValues(_.length)
  )
  val accuracy = EvaluationStatistics.calcPercentage(
    predictions.groupBy(_("correct")).mapValues(_.length)
  )
  val recall = numPredicted.toFloat/total.toFloat

  def toList: List[(String, String)] = {
    List(
      ("total", s"$total"),
      ("num predicted", s"$numPredicted"),
      ("num correct", s"$numCorrect"),
      ("accuracy", s"$accuracy"),
      ("precision", s"$precision"),
      ("recall", s"$recall")
    )
  }
}
