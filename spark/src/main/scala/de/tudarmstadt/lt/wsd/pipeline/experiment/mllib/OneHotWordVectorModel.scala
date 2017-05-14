package de.tudarmstadt.lt.wsd.pipeline.experiment.mllib

import de.tudarmstadt.lt.wsd.pipeline.feature.StopWordsFilter
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.{OneHotEncoder, StringIndexer, StringIndexerModel}
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.Dataset

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
class OneHotWordVectorModel(override val uid: String) extends AbstractWordVectorModel {
  override def copy(extra: ParamMap): OneHotWordVectorModel = defaultCopy(extra)

  def this() = this(Identifiable.randomUID("oneHotWordVectorizer"))

  def train(dataset: Dataset[_]): OneHotWordVectorModel = {
    assert($(featureCol) == null, "Feature column must not be set for this model.")
    val vocabulary = dataset.select($(inputCol)).dropDuplicates($(inputCol))

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
    val pipeline = new Pipeline().setStages(stages)

    val model = pipeline.fit(vocabulary)

    import dataset.sparkSession.implicits._
    // TODO performance by not collecting but joining, see http://stackoverflow.com/a/35257145
    // for broadcasting dataframes
    val labels = model.stages{1}.asInstanceOf[StringIndexerModel].labels
    // Roughly inspired by sparks StringIndexer.scala
    val indexed = labels.zipWithIndex.map{
      case (s: String, i: Int) => (s,i)
    }
    val featureLabels = dataset.sparkSession.createDataset(indexed).repartition(1)

    val wordVectors = model.transform(vocabulary)
    this
  }

  def save(path: String) = ???
}
