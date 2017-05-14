package de.tudarmstadt.lt.wsd.pipeline.experiment.mllib

import org.apache.spark.ml._
import org.apache.spark.ml.feature.LabeledPoint
import org.apache.spark.ml.param.{Param, ParamMap, Params}
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Dataset}


// TODO: Is it sensible that the vectorizer inherits input and outputCol?
trait SharedWordVectorParams extends Params {
  final val inputCol: Param[String] = new Param[String](this, "inputCol", "")
  final def getInputCol: String = $(inputCol)
  def setInputCol(value: String): this.type = set(inputCol, value)

  final val outputCol: Param[String] = new Param[String](this, "outputCol", "Column name for the created vector")
  //setDefault(outputCol, "wordVector")
  final def getOutputCol: String = $(outputCol)
  def setOutputCol(value: String): this.type = set(outputCol, value)
}

abstract class AbstractWordVectorModel extends SharedWordVectorParams {
  final val featureCol: Param[String] = new Param[String](this, "featureCol", "")
  final def getFeatureCol: String = $(featureCol)
  def setFeatureCol(value: String): this.type = set(featureCol, value)

  protected var wordVectors: Dataset[LabeledPoint] = _

  // FIXME copy params, but only from SharedWordVectorParams
  def transformer: WordVectorizer = new WordVectorizer(uid, wordVectors)
}

class WordVectorizer(override val uid: String, wordVectors: Dataset[LabeledPoint])
  extends Transformer with SharedWordVectorParams {
  override def copy(extra: ParamMap): WordVectorizer = defaultCopy(extra)

  override def transform(dataset: Dataset[_]): DataFrame = {
    // join with word vectors
    dataset.join(wordVectors.as("wv")).where(col($(inputCol)) === col(s"wv.${$(inputCol)}"))
  }

  override def transformSchema(schema: StructType): StructType = ???
}