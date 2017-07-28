package de.tudarmstadt.lt.wsd.common.prediction

import com.typesafe.scalalogging.LazyLogging
import de.tudarmstadt.lt.wsd.common.model.{Sense, SenseVector}
import de.tudarmstadt.lt.wsd.common.{Feature, FeatureExtractor, FeatureVectorizer, LemmaFeatureExtractor}
import breeze.linalg.{Vector => BV}
import de.tudarmstadt.lt.wsd.common.prediction.DetailedPredictionPipeline.Predictions

trait PredictWordInContextPipeline {
  def predictWordInContext(word: String, context: String): Predictions
}

class DetailedPredictionPipeline(val model: WSDModel) extends PredictWordInContextPipeline
  with LazyLogging {
  import DetailedPredictionPipeline._

  val extractor: FeatureExtractor = LemmaFeatureExtractor // FIXME should depend on model, might be coupled to word vector?
  val vectorizer: FeatureVectorizer = FeatureVectorizer.getVectorizer(model.word_vector_model)
  val classifierGenerator: (String) => WordSenseClassifier = WordSenseClassifiers.getGenerator(model)

  def mutualComponents(senseVector: BV[Double], contextVector: BV[Double]): Seq[Feature] = {
    val hadamardProduct = VectorDistanceMeasure.hadamardProduct(senseVector, contextVector)
    vectorizer.doUnvectorize(hadamardProduct)
  }

  val extractFeatures: Pipeline = (data: Data) =>
    data + (CONTEXT_FEATURES_COL -> extractor.extractFeatures(
      data(CONTEXT_COL).asInstanceOf[String],
      data(TARGET_COL).asInstanceOf[String]))

  val vectorizeFeatures: Pipeline = (data: Data) =>
    data + (CONTEXT_VECTOR_COL -> vectorizer.doVectorize(
      data(CONTEXT_FEATURES_COL).asInstanceOf[List[String]].toArray))

  val preprocess: Pipeline = extractFeatures.andThen(vectorizeFeatures)

  val pipelineGenerator: (String) => Pipeline = (word: String) => {
    val classifier = classifierGenerator(word)
    val classify = (data: Data) => {
      val contextVector = data(CONTEXT_VECTOR_COL).asInstanceOf[breeze.linalg.Vector[Double]]
      val predictions = classifier.classify(contextVector)
      data + (
        PREDICT_SENSES_COL -> predictions.map(_.sense),
        PREDICT_SCORE_COL -> predictions.map(_.score),
        MUTUTAL_FEATURES_COL -> predictions.map(p =>
          mutualComponents(p.sense.vector, contextVector)),
        SENSE_FEATURES_COL -> predictions.map(p =>
          vectorizer.doUnvectorize(p.sense.vector))
      )
    }
    preprocess.andThen(classify)
  }

  def predictWordInContext(word: String, context: String): Predictions = {
    val pipeline = pipelineGenerator(word)
    val input = Map(
      TARGET_COL -> word,
      CONTEXT_COL -> context
    )
    val output = pipeline(input)
    Predictions(output, model)
  }
}


class EnsemblePredictionPipeline(val models: Seq[WSDModel])
  extends PredictWordInContextPipeline {
  private val pipelines = models.map(new DetailedPredictionPipeline(_))

  def predictWordInContext(word: String, context: String): Predictions = {
    val fallbackPipeline = pipelines.last
    // Use iterator to only evaluate until first hasPosScore for better performance!
    val pipelinesIter = pipelines.dropRight(1).iterator
    val optPredictions = pipelinesIter.map(_.predictWordInContext(word, context)).find(_.hasPosScore())

    // Fall back to last
    optPredictions.getOrElse(fallbackPipeline.predictWordInContext(word, context))
  }
}

object DetailedPredictionPipeline {

  type Data = Map[String, Any]
  type Pipeline = (Data) => Data

  val CONTEXT_COL = "context"
  val TARGET_COL = "target"

  val PREDICT_SENSES_COL = "predict_senses"
  val PREDICT_SCORE_COL = "predict_score"

  val CONTEXT_FEATURES_COL = "context_features"
  val CONTEXT_VECTOR_COL = "context_vector"
  val MUTUTAL_FEATURES_COL = "mutual_features"
  val SENSE_FEATURES_COL = "sense_features"

  val REQUIRED_INPUT_COL_SET = Set(TARGET_COL, CONTEXT_COL)

  val REJECT_OPTION_ID = "-1"

  def get(model: WSDModel): PredictWordInContextPipeline = {
    if (model.classifier == WordSenseClassifiers.ensemble)
      bestEnsembledPipeline
    else
      new DetailedPredictionPipeline(model)
  }

  def bestEnsembledPipeline: EnsemblePredictionPipeline = {
    val models = Seq(
      WSDModel.parseFromString("cos_traditional_coocwords"),
      WSDModel.parseFromString("cos_traditional_self"),
      WSDModel.parseFromString("cos_cosets1k_self")
    )
    new EnsemblePredictionPipeline(models)
  }


  case class Predictions(
     context: String,
     contextFeatures: Seq[String],
     word: String,
     model: WSDModel,
     senses: Seq[SenseVector],  // FIXME to Sense and mutual features in other field
     scores: Seq[Double],
     mutualFeatures: Seq[Seq[Feature]],
     senseFeatures: Seq[Seq[Feature]]
) {
    def hasPosScore(): Boolean = scores.exists(_ > 0.0)

    def transpose(): Seq[SingleSensePrediction] = {
      val scoreSum = scores.sum
      def calcConfidence(score: Double) = if (scoreSum == 0.0) 0.0 else score / scoreSum
      val transposed = Seq(senses, scores, mutualFeatures, senseFeatures).transpose
      val sorted = transposed.sortBy{
        case Seq(_, score, _, _) => - score.asInstanceOf[Double]
      }
      sorted.zipWithIndex.map {
        case (Seq(sense, score, mutFeats, senseFeats), rank) =>
          SingleSensePrediction(
            sense = sense.asInstanceOf[SenseVector],
            model = model,
            score = score.asInstanceOf[Double],
            mutualFeatures = mutFeats.asInstanceOf[Seq[Feature]],
            senseFeatures = senseFeats.asInstanceOf[Seq[Feature]],
            rank = rank,
            confidence = calcConfidence(score.asInstanceOf[Double])
          )
      }
    }
  }

  object Predictions {
    def apply(data: Data, model: WSDModel): Predictions = {
      Predictions(
        context = data(CONTEXT_COL).asInstanceOf[String],
        contextFeatures = data(CONTEXT_FEATURES_COL).asInstanceOf[List[String]],
        word = data(TARGET_COL).asInstanceOf[String],
        model = model,
        senses = data(PREDICT_SENSES_COL).asInstanceOf[Seq[SenseVector]],
        scores = data(PREDICT_SCORE_COL).asInstanceOf[Seq[Double]],
        mutualFeatures = data(MUTUTAL_FEATURES_COL).asInstanceOf[Seq[Seq[Feature]]],
        senseFeatures =  data(SENSE_FEATURES_COL).asInstanceOf[Seq[Seq[Feature]]]
      )
    }
  }

  case class SingleSensePrediction(
    sense: SenseVector,
    model: WSDModel,
    score: Double,
    mutualFeatures: Seq[Feature],
    senseFeatures: Seq[Feature],
    rank: Int,
    confidence: Double)

}