package controllers
import play.api.libs.json._

object Implicits {
  import de.tudarmstadt.lt.wsd.common.JsonImplicits._

  implicit val wordQueryReads: Reads[PredictionQuery] = Json.reads[PredictionQuery]
  implicit val detectEntitiesQueryReads: Reads[DetectEntitiesQuery] = Json.reads[DetectEntitiesQuery]
  implicit val resultWrites: OWrites[Result] = Json.writes[Result]
  implicit val wordVectorWrites: OWrites[WordVector] = Json.writes[WordVector]
  implicit val clusterWordFeatureWrites: OWrites[ClusterWordFeature] = Json.writes[ClusterWordFeature]
  implicit val featureDetailsWrites: OWrites[FeatureDetails] = Json.writes[FeatureDetails]

}
