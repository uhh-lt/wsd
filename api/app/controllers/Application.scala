package controllers

import de.tudarmstadt.lt.wsd.common.JsonImplicits._
import play.api.libs.json._
import java.net.{URLClassLoader, URLDecoder, URLEncoder}
import javax.inject._

import com.typesafe.config.ConfigFactory
import de.tudarmstadt.lt.wsd.common.{DetectEntities, Feature, FeatureVectorizer}
import de.tudarmstadt.lt.wsd.common.model.{SampleSentence, Sense, SenseVector, WordVector => WordVectorModel}
import play.api.mvc.{Result => MvcResult, _}
import de.tudarmstadt.lt.wsd.common.prediction.DetailedPredictionPipeline.{Predictions, SingleSensePrediction}
import de.tudarmstadt.lt.wsd.common.prediction.{DetailedPredictionPipeline, WSDModel}
import de.tudarmstadt.lt.wsd.common.utils.NLPUtils
import scalikejdbc._

import scala.util.Try

@Singleton
class Application @Inject() extends Controller {
  import Implicits._
  import de.tudarmstadt.lt.wsd.common.JsonImplicits._

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

      // FIXME remove lazy and handle empty sense list case
      lazy val sentencesPerSense = {
        val ss = SampleSentence.defaultAlias
        val buildCondition = (s: Sense) => sqls.eq(ss.sense_id, s.sense_id).and.eq(ss.inventory, s.inventory)

        val parts = prediction.senses.map(_.sense).map(buildCondition)
        val criteria = parts.foldLeft(sqls""){
          case (where, part) if where.isEmpty => part
          case (where, part) => where.or(part)
        }

        val values = SampleSentence.findAllBy(criteria).groupBy(s => (s.sense_id, s.inventory))
        values.withDefaultValue(List())
      }

      val addSampleSentences = (__ \ 'predictions).json.update(
        __.read[JsArray].map { a =>
          Json.toJson(a.as[List[JsObject]].map { o =>
            val senseCluster = (o \ "senseCluster").as[JsObject]
            val senseId = (senseCluster \ "id").as[String]
            val inventory = (senseCluster \ "inventory").as[String]
            val uniqueSenseKey = (senseId, inventory)

            o ++ Json.obj(
              "senseCluster" -> (senseCluster ++ Json.obj(
                "sampleSentences" -> sentencesPerSense(uniqueSenseKey)
              ))
            )
          })
        }
      )

      val jsonResult = Json.toJson(Result(prediction))
        .transform(addSampleSentences)
        .flatMap(_.transform(addImageUrl))

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

      case Some(sense) =>

        val wordVectors = sense.sense.cluster_words.flatMap{ w =>
          WordVectorModel.findByModelAndWord(model.word_vector_model, w)}

        val featureWeightInWordVector = wordVectors.map{ v =>
          (v.word, vectorizer.doUnvectorize(v.vector).find(_.label == feature))
        }.filter(_._2.nonEmpty)

        val clusterWordWeights = sense.sense.weighted_cluster_words
          .map(x => (x.word, x.weight)).toMap

        val normalizationFactor = featureWeightInWordVector.map {
          case (word, Some(Feature(_, wordVectorFeatureWeight))) => wordVectorFeatureWeight * clusterWordWeights(word)
        }.sum


        val clusterFeatures = featureWeightInWordVector.toList.map {
          case (word, Some(Feature(_, featureWeight))) =>

            val weightContribution = clusterWordWeights(word) * featureWeight / normalizationFactor

            FeatureContribution(
              clusterWord = word,
              weightContribution = weightContribution
            )
        }.sortBy(-_.weightContribution)

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
  clusterFeatures: List[FeatureContribution])

case class FeatureContribution(
  clusterWord: String,
  weightContribution: Double)

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