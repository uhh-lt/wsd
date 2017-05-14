package de.tudarmstadt.lt.wsd.pipeline

import de.tudarmstadt.lt.wsd.common.model.{IndexedFeature, Sense, SenseInventory}
import de.tudarmstadt.lt.wsd.common.utils.FileUtils
import de.tudarmstadt.lt.wsd.pipeline.feature.{OneHotEncoder, StopWordsFilter, WeightedStringGroupingVectorizer}
import de.tudarmstadt.lt.wsd.pipeline.model.{CSVImporter, ContextFeature, SenseVector, WordVector}
import de.tudarmstadt.lt.wsd.pipeline.sql.functions._
import de.tudarmstadt.lt.wsd.pipeline.utils.RuntimeContext
import de.tudarmstadt.lt.wsd.pipeline.utils.SparkUtils._
import org.apache.spark.ml.feature.{DistributedStringIndexer, DistributedStringIndexerModel}
import org.apache.spark.ml.{Model, Pipeline}
import org.apache.spark.ml.param.{Param, ParamMap, Params}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.{DataFrame, Dataset, SaveMode}
import org.apache.spark.sql.functions._

trait SenseVectorModelBuilderParams extends Params {
  override val uid: String = Identifiable.randomUID("senseVectorModelBuilderParams")

  final val modelLocation: Param[String] = new Param[String](this, "modelLocation", "")
  def setModelLocation(value: String): this.type = set(modelLocation, value)

  final val contextFeaturesCVSPath: Param[String] = new Param[String](this, "contextFeaturesCVSPath", "")
  def setContextFeaturesCVSPath(value: String): this.type = set(contextFeaturesCVSPath, value)

  final val senseClustersCVSPath: Param[String] = new Param[String](this, "senseClustersCVSPath", "")
  def setSenseClustersCVSPath(value: String): this.type = set(senseClustersCVSPath, value)

  final val wordVectorFeature: Param[String] = new Param[String](this, "wordVectorFeature", "")
  def setWordFeature(value: String): this.type = set(wordVectorFeature, value)

  final val inventoryName: Param[String] = new Param[String](this, "inventoryName", "")
  def setInventoryName(value: String): this.type = set(inventoryName, value)

  def wordVectorModelName = s"${$(wordVectorFeature)}"

  def senseVectorModelName = s"${$(inventoryName)}_$wordVectorModelName"

  protected def getWordVectorsParquetPath = s"${$(modelLocation)}/word_vectors/$wordVectorModelName"
  protected def getSenseInventoryParquetPath = s"${$(modelLocation)}/sense_inventories/${$(inventoryName)}"
  protected def getSenseVectorsParquetPath = s"${$(modelLocation)}/sense_vectors/$senseVectorModelName"
  protected def getContextFeaturesParquetPath = s"${$(modelLocation)}/context_features"

  override def copy(extra: ParamMap): Params = defaultCopy(extra)
}

/**
  * Created by fide on 07.04.17.
  */
class SenseVectorModelBuilder(override val uid: String) extends RuntimeContext with SenseVectorModelBuilderParams {
  def this() = this(Identifiable.randomUID("wsdModelBuilder"))

  import spark.implicits._

  var contextFeatures: Dataset[ContextFeature] = _
  var senses: DataFrame = _
  var wordVectorsFeatureIndex: Dataset[IndexedFeature] = _
  var wordVectors: Dataset[WordVector] = _
  var senseVectors: Dataset[SenseVector] = _

  var contextFeaturesIsNew: Boolean = _
  var sensesIsNew: Boolean = _
  var wordVectorsIsNew: Boolean = _
  // senseVectors will always be new

  private def loadSenseInventory(): Unit = {
    val parquetPath = getSenseInventoryParquetPath
    if (FileUtils.existsFile(parquetPath)) {
      sensesIsNew = false
      senses = spark.read.parquet(parquetPath)// TODO unclear if good or not to do .as[Sense]
    } else {
      sensesIsNew = true
      senses = if ($(inventoryName) == Sense.traditional.toString) {
        CSVImporter.loadTraditionalSenseClusters($(senseClustersCVSPath))
      } else {
        CSVImporter.loadCoSetSenseClusters($(senseClustersCVSPath))
      }
      senses = senses.withColumn("inventory", lit($(inventoryName)))
    }
  }
  private def loadContextFeatures(): Unit = {
    val parquetPath = getContextFeaturesParquetPath

    if (FileUtils.existsFile(parquetPath)) {
      contextFeaturesIsNew = false
      contextFeatures = spark.read.parquet(parquetPath).as[ContextFeature]
    } else {
      contextFeaturesIsNew = true
      contextFeatures = CSVImporter.loadContextFeatures($(contextFeaturesCVSPath))
    }
    contextFeatures = contextFeatures.repartition(200) // TODO not yet tested
  }

  private def loadWordVectors(): Unit = {
    val parquetPath = getWordVectorsParquetPath

    def indexerModelToIndexFeatureDataset(model: DistributedStringIndexerModel) = {
      model.labels.map{
        case (s: String, i: Double) => IndexedFeature.apply(s,i.toInt, wordVectorModelName)
      }
    }

    def trainSelfModel() = {
      val vocabulary = contextFeatures.select("word").dropDuplicates("word")

      val removeStopWords = new StopWordsFilter()
        .setFilterCol("word")
      val indexer = new DistributedStringIndexer()
        .setInputCol("word")
        .setOutputCol("word__idx")
      val vectorizer = new OneHotEncoder()
        .setInputCol("word__idx")
        .setOutputCol("vector")
        .setDropLast(false)

      val stages = Array(removeStopWords, indexer, vectorizer)
      val pipeline = new Pipeline().setStages(stages)

      val model = pipeline.fit(vocabulary)

      wordVectorsFeatureIndex = indexerModelToIndexFeatureDataset(
        model.stages{1}.asInstanceOf[DistributedStringIndexerModel]
      )

      model
        .transform(vocabulary)
        .select("word", "vector")
        .as[WordVector]
    }

    def trainGroupingModel(feature: String) = {
      val featurCol = feature match {
        case "coocwords" => "head"
        case "coocdeps" => "holing"
      }


      val removeStopWords = new StopWordsFilter()
        .setFilterCol("head")
      val punctuation_marks = Array("!", "?", "(", ")", ".", ",",
        ":", ";", "\"", "-", "_", "–", "`", "´", "/")
      val removePunctuation = new StopWordsFilter()
        .setFilterCol("head") // In both cases head is correct
        .setStopWords(punctuation_marks)
      val indexer = new DistributedStringIndexer()
        .setInputCol(featurCol)   // holing => head, dep, deptype
        .setOutputCol("feature__idx")
      val vectorizer = new WeightedStringGroupingVectorizer()
        .setFeatureIdxCol("feature__idx")
        .setGroupByCol("word")
        .setWeightCol("weight")
        .setNormalizeVector(true)
        .setKeepOtherCols(false)

      val model = new Pipeline()
        .setStages(Array(removeStopWords, removePunctuation, indexer, vectorizer))
        .fit(contextFeatures)

      wordVectorsFeatureIndex = indexerModelToIndexFeatureDataset(
        model.stages{2}.asInstanceOf[DistributedStringIndexerModel]
      )

      model
        .transform(contextFeatures)
        .select("word", "vector")
        .as[WordVector]
    }

    if (FileUtils.existsFile(parquetPath)) {
      wordVectorsIsNew = false
      wordVectorsFeatureIndex = spark.read.parquet(parquetPath + "/index").as[IndexedFeature]
      wordVectors = spark.read.parquet(parquetPath + "/vectors").as[WordVector]
    } else {
      assert(contextFeatures != null, "Context features must be loaded")
      wordVectorsIsNew = true
      wordVectors =  if ($(wordVectorFeature) == "self")
        trainSelfModel()
      else {
        trainGroupingModel($(wordVectorFeature))
      }
    }
  }

  private def buildSenseVectors(): Unit = {
    assert(wordVectors != null, "Word vectors must be loaded")
    assert(senses != null, "Sense inventory must be loaded")

    senseVectors = senses.as("sc")
      // Wide to long DataFrame, by exploding on array of weighted cluster words
      .withColumn("weighted_cluster_word", explode('weighted_cluster_words))
      .withColumn("cluster_word", getFirstAsStringUDF('weighted_cluster_word))
      .withColumn("cluster_word_weight", getSecondAsDoubleUDF('weighted_cluster_word))
      // join with word vectors
      .join(wordVectors.as("wv"))
      .where($"cluster_word" === $"wv.word")
      // weight, aggregate and normalize vectors
      .withColumn("weighted_sense_vector", scaleVectorUDF(col("cluster_word_weight"), col("wv.vector")))
      .groupBy('sense_id)
      .agg(norm_vec_sum('weighted_sense_vector).as("sense_vector"))
      .select("sense_id", "sense_vector")
      .withColumnRenamed("sense_vector", "vector")
      .select("sense_id", "vector")
      .as[SenseVector]

  }

  private def saveSenseVectors(saveMode: SaveMode = SaveMode.Overwrite) =
    senseVectors.write.mode(saveMode).parquet(getSenseVectorsParquetPath)

  private def saveWordVectors(saveMode: SaveMode = SaveMode.Overwrite) = {
    wordVectorsFeatureIndex.write.mode(saveMode).parquet(getWordVectorsParquetPath + "/index")
    wordVectors.write.mode(saveMode).parquet(getWordVectorsParquetPath + "/vectors")
  }

  private def saveContextFeatures(saveMode: SaveMode = SaveMode.Overwrite) =
    contextFeatures.write.mode(saveMode).parquet(getContextFeaturesParquetPath)

  private def saveSenseInventory(saveMode: SaveMode = SaveMode.Overwrite) =
    senses.write.mode(saveMode).parquet(getSenseInventoryParquetPath)

  private def saveNew(saveMode: SaveMode = SaveMode.Overwrite): Unit = {
    if (contextFeaturesIsNew) saveContextFeatures(saveMode)
    if (wordVectorsIsNew) saveWordVectors(saveMode)
    if (sensesIsNew) saveSenseInventory(saveMode)
    saveSenseVectors(saveMode)
  }

  def load(): this.type = {
    loadContextFeatures()
    loadSenseInventory()
    loadWordVectors()
    this
  }

  def build(): this.type = {
    buildSenseVectors()
    this
  }

  def save(): Unit = {
    saveNew()
  }

}