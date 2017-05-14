package controllers

import Implicits._
import play.api.libs.json._
import de.tudarmstadt.lt.wsd.common._
import java.net.{URLClassLoader, URLDecoder, URLEncoder}
import javax.inject._

import com.typesafe.config.ConfigFactory
import de.tudarmstadt.lt.wsd.common.model.{SenseVector, WordVector => WordVectorModel}
import play.api.mvc.{Result => MvcResult, _}
import de.tudarmstadt.lt.wsd.common.prediction.DetailedPredictionPipeline.{Predictions, SingleSensePrediction}
import de.tudarmstadt.lt.wsd.common.prediction.{DetailedPredictionPipeline, WSDModel}
import de.tudarmstadt.lt.wsd.common.utils.NLPUtils

@Singleton
class Application  @Inject() extends Controller {
  import Implicits._

  private val config = ConfigFactory.load()

  def index = Action {
    Ok(NLPUtils.toString + " # " +Thread.currentThread().getId())
  }

  def runOnSuccess[T](result: JsResult[T])(block: (T) => MvcResult): MvcResult = {
    result.fold(
      errors => BadRequest(Json.obj("status" ->"ERROR", "message" -> JsError.toJson(errors))),
      query => block(query)
    )
  }

  def postPredictionQuery: Action[JsValue] = Action(BodyParsers.parse.json) { request =>
    val queryResult = request.body.validate[PredictionQuery]

    runOnSuccess(queryResult){ query: PredictionQuery =>

      val pipeline = DetailedPredictionPipeline.get(query.model)
      // TODO val actualModelName = result.modelName
      // TODO make changable to flickr
      // TODO handle lemmatization if not found

      val prediction = pipeline.predictWordInContext(
        word = query.word,
        context = query.context
      )

      val publicUrl = config.getString("wsd.api.public_url")
      val imageUrl = (senseId: String, inventory: String) => {
        val senseIdPart = URLEncoder.encode(senseId, "UTF-8")
        s"$publicUrl/bing-photo/$inventory/$senseIdPart"
      }


      val addImageUrl = (__ \ 'predictions).json.update(
        __.read[JsArray].map { a =>
          Json.toJson(a.as[List[JsObject]].map { o =>
            val senseId = (o \ "senseCluster" \ "id").as[String]
            val inventory = (o \ "senseCluster" \ "inventory").as[String]
            o ++ Json.obj("imageUrl" -> imageUrl(senseId, inventory))
          })
        }
      )

      val jsonResult = Json.toJson(Result(prediction)).transform(addImageUrl)

      Ok(jsonResult.getOrElse(JsString("Oopps.. an error occurred")))
    }
  }

  def postDetectNamedEntitiesQuery: Action[JsValue] = Action(BodyParsers.parse.json) { request =>
    val queryResult = request.body.validate[DetectEntitiesQuery]

    runOnSuccess(queryResult){ query: DetectEntitiesQuery =>
      val tokensWithNEs = DetectEntities.get(query.sentence)
      val result = tokensWithNEs.zipWithIndex.map{ case ((token, entityName), id) =>
        Json.obj(
          "id" -> id,
          "text" -> token,
          "isEntity" -> JsBoolean(entityName != NLPUtils.OTHER_TAG),
          "type" -> entityName
        )
      }

      Ok(Json.toJson(result))
    }
  }


  def getWordVector(modelName: String, word: String) = Action {
    val model = WSDModel.parseFromString(modelName)
    val vectorizer = FeatureVectorizer.getVectorizer(model.word_vector_model)
    val result = WordVectorModel.findAllByModelAndWord(model.word_vector_model, word)

    result.headOption match {
      case Some(row) =>
        val labelWithWeights = vectorizer.doUnvectorize(row.vector)
        val wordVector = WordVector(row.word, labelWithWeights.toList)
        Ok(Json.toJson(wordVector))

      case _ => NotFound(s"<h1>WordVector for word '$word' not found!</h1>")
    }
  }

  def getFeatureDetails(modelName: String, feature: String, encodedSenseID: String) = Action {
    val model = WSDModel.parseFromString(modelName)
    val vectorizer = FeatureVectorizer.getVectorizer(model.word_vector_model)

    val senseID = URLDecoder.decode(encodedSenseID)

    SenseVector.findByModelAndId(model.sense_vector_model, senseID) match {

      case Some(cluster) =>
        val wordVectors = cluster.sense.cluster_words.flatMap{ w =>
          WordVectorModel.findByModelAndWord(model.word_vector_model, w)}

        val clusterFeatureWeights = wordVectors.map{ v =>
          (v.word, vectorizer.doUnvectorize(v.vector).find(_.label == feature))
        }.filter(_._2.nonEmpty)

        val wordWeights = cluster.sense.cluster_words.map((_, -1.0)).toMap
        // FIXME val wordWeights = cluster.sense.weighted_words.toMap

        val clusterFeatures = clusterFeatureWeights.toList.map{
          case (word, Some(Feature(_, featureWeight))) =>
            ClusterWordFeature(word, wordWeights(word), featureWeight)
        }
        val details = FeatureDetails(senseID, feature, clusterFeatures)
        Ok(Json.toJson(details))

      case _ => NotFound(s"<h1>SenseCluster '$senseID' not found!</h1>")
    }
  }
}

case class PredictionQuery(word: String, context: String, model: WSDModel)
case class DetectEntitiesQuery(sentence: String)
case class WordVector(word: String, features: List[Feature])

case class FeatureDetails(
  senseID: String,
  feature: String,
  clusterFeatures: List[ClusterWordFeature])

case class ClusterWordFeature(
  clusterWord: String,
  clusterWordWeight: Double,
  featureWeight: Double)



case class Result(
   context: String,
   word: String,
   contextFeatures: Seq[String],
   modelName: String,
   predictions: Seq[SingleSensePrediction])

object Result {
  def apply(prediction: Predictions): Result = {
    val predictions = prediction.transpose().take(20)
    Result(
      context = prediction.context,
      word = prediction.word,
      contextFeatures = prediction.contextFeatures,
      modelName = prediction.model.toString,
      predictions = predictions
    )
  }
}