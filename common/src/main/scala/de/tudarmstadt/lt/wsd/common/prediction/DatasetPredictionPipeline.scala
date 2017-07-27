package de.tudarmstadt.lt.wsd.common.prediction

import com.typesafe.scalalogging.slf4j.LazyLogging
import de.tudarmstadt.lt.wsd.common.{FeatureExtractor, FeatureVectorizer, LemmaFeatureExtractor, OnlyDepFromHolingExtractor}
import de.tudarmstadt.lt.wsd.common.prediction.DetailedPredictionPipeline._
import de.tudarmstadt.lt.wsd.common.utils.{FileUtils, TSVUtils}

/**
  * Created by fide on 25.04.17.
  */
class DatasetPredictionPipeline(model: WSDModel, runConfig: RunConfig)
  extends LazyLogging {
  import DatsetPredictionPipeline._

  val extractor: FeatureExtractor = LemmaFeatureExtractor // FIXME should depend on model, might be coupled to word vector?
  val vectorizer: FeatureVectorizer = FeatureVectorizer.getVectorizer(model.word_vector_model)
  val classifierGenerator: (String) => WordSenseClassifier = WordSenseClassifiers.getGenerator(model)

  val extractFeatures: Pipeline = (data: Data) => {
    if (runConfig.onlyDirectDepFeatures) {
      data + (CONTEXT_FEATURES_COL -> OnlyDepFromHolingExtractor.extractFeatures(
        data(TARGET_HOLING_FEATURE_COL).asInstanceOf[String],
        data(TARGET_COL).asInstanceOf[String]))
    } else {
      data + (CONTEXT_FEATURES_COL -> extractor.extractFeatures(
        data(CONTEXT_COL).asInstanceOf[String],
        data(TARGET_COL).asInstanceOf[String]))
    }
  }

  val vectorizeFeatures: Pipeline = (data: Data) =>
    data + (CONTEXT_VECTOR_COL -> vectorizer.doVectorize(
      data(CONTEXT_FEATURES_COL).asInstanceOf[List[String]].toArray))

  val preprocess: Pipeline = extractFeatures.andThen(vectorizeFeatures)

  val pipelineGenerator: (String) => Pipeline = (word: String) => {
    val classifier = classifierGenerator(word)
    val classify = (data: Data) => {
      val contextVector = data(CONTEXT_VECTOR_COL).asInstanceOf[breeze.linalg.Vector[Double]]
      val predictions = classifier.classify(contextVector)
      val firstOpt = predictions.headOption
      data + (
        PREDICT_SENSE_IDS_COL -> firstOpt.map(_.sense.sense_id).getOrElse(REJECT_OPTION_ID),
        PREDICT_RELATED_COL -> firstOpt.map(_.sense.sense.hypernyms.mkString(",")).getOrElse(""),
        PREDICT_SCORE_COL -> firstOpt.map(_.score.toString).getOrElse("")
      )
    }
    preprocess.andThen(classify)
  }

  def predictDatasetFileWithAllWords(input: String, resultFolder: String): Unit = {
    val (headers, datasets) = TSVUtils.readWithHeaders(input)
    val groupedByWord = datasets.groupBy(_(TARGET_COL))

    // TODO val numSenseIDsMap = Sense.findAll().map(x => x.sense_id -> x).toMap
    // TODO val filterSenses = config.getSenseFilter(model)

    groupedByWord.par.foreach{ case (word: String, dataset: DataSet) =>
      val wordFile = s"$resultFolder/$word.csv"
      if (runConfig.skipOnWordFileExits && FileUtils.existsFile(wordFile)) {
        logger.info(s"$wordFile exists and skipping mode is on, skipping word!")
      } else {
        val predictedDataset = predictWordSpecificDataset(word, dataset)
        val outputDataset = predictedDataset
          .map(_.filterKeys(OUTPUT_COL_SET))
          .map(_.asInstanceOf[Map[String, String]]) // these are all string columns
        TSVUtils.writeWithHeaders(wordFile, outputDataset, OUTPUT_COL_SET.toSeq)
      }
    }
  }

  def predictWordSpecificDataset(word: String, dataset: DataSet): DataSet = {
    val pipeline = pipelineGenerator(word)
    // TODO val isOutOfVoc = allSenses.isEmpty
    // TODO val senses = filterSenses(allSenses, word)
    dataset.map(pipeline)
  }
}

object DatsetPredictionPipeline {

  type Data = Map[String, Any]
  type Pipeline = (Data) => Data
  type DataSet = List[Data]

  val CONTEXT_ID_COL = "context_id"
  val CONTEXT_COL = "context"
  val TARGET_COL = "target"
  val TARGET_POS_COL = "target_pos"
  val TARGET_POSITION_COL = "target_position"

  val GOLD_SENSE_IDS_COL = "gold_sense_ids"
  val GOLDEN_RELATED_COL = "golden_related"

  val PREDICT_SENSE_IDS_COL = "predict_sense_ids"
  val PREDICT_RELATED_COL = "predict_related"
  val PREDICT_SCORE_COL = "predict_score"

  val TARGET_HOLING_FEATURE_COL = "target_holing_features"

  val CONTEXT_FEATURES_COL = "context_features"
  val CONTEXT_VECTOR_COL = "context_vector"

  val INPUT_COL_SET = Set(CONTEXT_ID_COL, TARGET_COL, TARGET_POS_COL,
    TARGET_POSITION_COL, GOLD_SENSE_IDS_COL, PREDICT_SENSE_IDS_COL,
    GOLDEN_RELATED_COL, PREDICT_RELATED_COL, CONTEXT_COL
  )
  val OUTPUT_COL_SET: Set[String] = INPUT_COL_SET union Set(PREDICT_SCORE_COL)
}

object RelatedType extends Enumeration {
  type Name = Value
  val words, hypernyms = Value
}

case class RunConfig(
  filterInventory: Option[String] = None,
  onlyDirectDepFeatures: Boolean,
  skipOnWordFileExits: Boolean,
  rejectOption: Boolean,
  useNumIDs: Boolean,
  relatedType: RelatedType.Value
)

