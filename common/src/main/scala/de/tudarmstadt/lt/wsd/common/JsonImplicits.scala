
package de.tudarmstadt.lt.wsd.common

import breeze.linalg.SparseVector
import de.tudarmstadt.lt.wsd.common.model.{Sense, SenseVector, WeightedWord}
import de.tudarmstadt.lt.wsd.common.prediction.DetailedPredictionPipeline.SingleSensePrediction
import de.tudarmstadt.lt.wsd.common.prediction._
import play.api.libs.json._

object JsonImplicits {

  implicit val weightedWodReads: Reads[WeightedWord] = Json.reads[WeightedWord]
  /*
  implicit val seqWeightedWodReads = new Reads[Seq[WeightedWord]] {
    def reads(js: JsValue): JsResult[Seq[WeightedWord]] = js match {
      case JsArray(array) => JsSuccess(array.map{
        case JsString(s) => s.split(':') match { case Array(a,b) => WeightedWord(a, b.toDouble) }
      })
      case _ => JsError("String value expected")
    }
  }
  */
  implicit val senseReads: Reads[Sense] = Json.reads[Sense]

  implicit val wsdModelTypeReads = new Reads[WSDModel] {
    def reads(js: JsValue): JsResult[WSDModel] = js match {
      case JsString(s) =>
        try {
          JsSuccess(WSDModel.parseFromString(s))
        } catch {
          case _: NoSuchElementException => JsError(
            s"Model '$s' does not exist."
          )
        }
      case _ => JsError("String value expected")
    }
  }

  implicit val vectorWrites = new Writes[SparseVector[Double]] {
    def writes(vec: SparseVector[Double]): JsObject = Json.obj(
      "keys" -> Json.arr(vec.activeKeysIterator.toSeq),
      "values" -> Json.arr(vec.activeValuesIterator.toSeq)
    )
  }

  implicit val clusterWrites = new Writes[SenseVector] {
    def writes(cluster: SenseVector): JsObject = Json.obj(
      "id" -> cluster.sense_id,
      "lemma" -> cluster.sense.word,
      "words" -> cluster.sense.cluster_words,
      "hypernyms" -> cluster.sense.hypernyms,
      "inventory" -> cluster.inventory,
      "model" -> cluster.model,
      "babelnet_id" -> cluster.sense.babelnet_id
    )
  }

  implicit val featureWrites: OWrites[Feature] = Json.writes[Feature]

  implicit val weightedWordWrite: OWrites[WeightedWord] = Json.writes[WeightedWord]
  implicit val senseWrites: OWrites[Sense] = Json.writes[Sense]


  implicit val predictionWriter = new Writes[SingleSensePrediction] {
    def writes(prediction: SingleSensePrediction): JsObject = Json.obj(
      "senseCluster" -> prediction.sense,
      "simScore" -> prediction.score,
      "rank" -> prediction.rank.toString,
      "confidenceProb" -> prediction.confidence,
      // TODO remove dirty hack
      "mutualFeatures" -> prediction.mutualFeatures.filterNot(_.weight.isNaN),
      "contextFeatures" -> "", //FIXME prediction.contextFeatures.map(f: String => Feature(f, 1)),
      "top20ClusterFeatures" -> prediction.senseFeatures.filterNot(_.weight.isNaN).take(20),
      "numClusterFeatures" -> prediction.senseFeatures.count(!_.weight.isNaN)
    )
  }
}
