package de.tudarmstadt.lt.wsd.pipeline.experiment.ml

import de.tudarmstadt.lt.wsd.pipeline.feature.StopWordsFilter
import org.apache.spark
import org.apache.spark.ml._
import org.apache.spark.ml.feature.{OneHotEncoder, StringIndexer, StringIndexerModel}
import org.apache.spark.ml.param.{Param, ParamMap, Params}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Dataset}

trait WordVectorizerParams extends Params {

  final val inputCol: Param[String] = new Param[String](this, "inputCol", "")
  final def getInputCol: String = $(inputCol)
  def setInputCol(value: String): this.type = set(inputCol, value)

  final val outputCol: Param[String] = new Param[String](this, "outputCol", "column name for the created vector")
  setDefault(outputCol, "wordVector")
  final def getOutputCol: String = $(outputCol)
  def setOutputCol(value: String): this.type = set(outputCol, value)

}

abstract class AbstractWordVectorizer
  extends Estimator[WordVectorizerModel] with WordVectorizerParams {

  final val featureCol: Param[String] = new Param[String](this, "featureCol", "")
  final def getFeatureCol: String = $(featureCol)
  def setFeatureCol(value: String): this.type = set(featureCol, value)


}

/*
 Thoughts about using sparks ML (i.e. Pipeline) architecture and/or sparks MLLib (Predictor) architecture
 Needed:
 - Distinction between model learner (ml.Predictor or ml.Estimator) and model (ml.PredictionModel or ml.Model or ml.Transformer)
 - Parametrization of model learner and model
 - Saving and loading models with params and data
 Not needed:
 - Using Pipelines ability of chaining PipelineStages (with Pipeline.fit() and Pipeline.tranfsorm()
 Unclear:
 - Was MLLib introduced with coupling to Pipeline architecture
 */
class OneHotWordVectorizer(override val uid: String) extends AbstractWordVectorizer {
  def this() = this(Identifiable.randomUID("oneHotWordVectorizer"))
  override def copy(extra: ParamMap): Estimator[WordVectorizerModel] = defaultCopy(extra)

  def pipeline: Pipeline = {
    val removeStopWords = new StopWordsFilter()
      .setFilterCol($(inputCol))
    val indexer = new StringIndexer()
      .setInputCol($(inputCol))
      .setOutputCol(s"${$(inputCol)}__idx")
    val vectorizer = new OneHotEncoder()
      .setInputCol(s"${$(inputCol)}__idx")
      .setOutputCol($(outputCol))
      .setDropLast(false)

    val stages = Array(removeStopWords, indexer, vectorizer)
    new Pipeline().setStages(stages)
  }

  override def fit(dataset: Dataset[_]): WordVectorizerModel = {
    val vocabulary = dataset.select($(inputCol)).dropDuplicates($(inputCol))

    val model = pipeline.fit(vocabulary)

    import dataset.sparkSession.implicits._
    val labels = model.stages{1}.asInstanceOf[StringIndexerModel].labels
    // Roughly inspired by sparks StringIndexer.scala
    val indexed = labels.zipWithIndex.map{
      case (s: String, i: Int) => (s,i) // TODO: unclear if good
    }
    val featureLabels = dataset.sparkSession.createDataset(indexed).repartition(1)

    val wordVectors = model.transform(vocabulary)
    new WordVectorizerModel(uid, wordVectors, featureLabels)
  }

  // Note: It seems quite unclear why transformSchema is needed for the Estimator, as it returns a Model and does
  // not return a Dataset/DataFrame of which it could have changed the schema.
  //
  // However the Pipeline.fit() method calls Pipeline.transformSchema() which in turn will call the tranformMethods of
  // all its PipelineStages which could potentialy contain Estimators. Addiontally the comment of the Pipeline.fit()
  // method helps understanding that the way Pipeline.fit() is implemtened Estimator.transformSchema must reflect the
  // schema changes the Model.transform will do
  override def transformSchema(schema: StructType): StructType = throw new UnsupportedOperationException("Please do not" +
    "use this Estimator as a PipelineStage, fit() expects another dataset schema than transform()")
}

class WordVectorizerModel(override val uid: String, val wordVectors: DataFrame,
  val featureLabels: Dataset[(String, Int)])
  extends Model[WordVectorizerModel] {
  override def copy(extra: ParamMap): WordVectorizerModel = ???

  override def transform(dataset: Dataset[_]): DataFrame = ???

  override def transformSchema(schema: StructType): StructType = ???
}