package de.tudarmstadt.lt.wsd.common.eval

import com.typesafe.scalalogging.LazyLogging
import de.tudarmstadt.lt.wsd.common.Run.{logger => _, _}
import de.tudarmstadt.lt.wsd.common.prediction.DetailedPredictionPipeline._
import de.tudarmstadt.lt.wsd.common.prediction.WSDModel
import de.tudarmstadt.lt.wsd.common.utils.TSVUtils

/**
  * Created by fide on 15.02.17.
  */
object Evaluator  extends LazyLogging {
  def eval(dataSetPath: String, inventoryPath: String, modelConfig: WSDModel): List[Map[String, String]] = {
    val (headers, rows) = TSVUtils.readWithHeaders(dataSetPath)
    val mapping = SenseInventoryMapping.loadFromTSVForModel(inventoryPath, modelConfig)
    logger.info(headers.mkString(", "))
    rows.map{r =>
      logger.info(r.map(x => s"${x._1} -> ${x._2}").mkString("\n"))
      val goldID = r("gold_sense_ids")  // external
      r("predict_sense_ids") match { // internal
        case REJECT_OPTION_ID =>
          r + (
            "correct"-> s"${false}", // FIXME fill with NA
            "predict_related_cleaned" -> "",
            "golden_related_cleaned" -> ""
            )
        case predictedID =>
          r + (
            "correct"-> s"${mapping.areIDsMapped(predictedID, goldID)}",
            "predict_related_cleaned" -> mapping.internalInventoryByID(predictedID).cleanedHypernyms.mkString(","),
            "golden_related_cleaned" -> mapping.externalInventoryByID(goldID).cleanedHypernyms.mkString(",")
            )
      }
    }.toList
  }

}
