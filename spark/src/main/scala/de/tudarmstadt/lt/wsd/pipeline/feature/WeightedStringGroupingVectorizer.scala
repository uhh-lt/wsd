package de.tudarmstadt.lt.wsd.pipeline.feature

import de.tudarmstadt.lt.wsd.pipeline.sql.{NormalizedVectorSumUDAF, VectorSumUDAF}
import de.tudarmstadt.lt.wsd.pipeline.utils.SchemaUtils
import org.apache.spark.annotation.{DeveloperApi, Experimental, Since}
import org.apache.spark.ml.feature.SQLTransformer
import org.apache.spark.ml.{Estimator, Model, Pipeline, PipelineModel}
import org.apache.spark.ml.param.{Param, ParamMap, Params}
import org.apache.spark.ml.stat.distribution.MultivariateGaussian
import org.apache.spark.ml.util.{DefaultParamsWritable, Identifiable}
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.ml.linalg.SQLDataTypes.VectorType

/**
  * Created by fide on 24.11.16.
  */

trait WeightedStringGroupingVectorizerParams extends Params {


  final val groupByCol: Param[String] = new Param[String](this, "groupByCol", "Column name to group by")
  final def getGroupByCol: String = $(groupByCol)
  def setGroupByCol(value: String): this.type = set(groupByCol, value)

  final val weightCol: Param[String] = new Param[String](this, "weightCol", "weight column name")
  final def getWeightCol: String = $(weightCol)
  def setWeightCol(value: String): this.type = set(weightCol, value)

  final val featureIdxCol: Param[String] = new Param[String](this, "featureIdxCol", "feature index column name")
  final def getFeatureIdxCol: String = $(featureIdxCol)
  def setFeatureIdxCol(value: String): this.type = set(featureIdxCol, value)

  final val normalizeVector: Param[Boolean] =
    new Param[Boolean](this, "normalizeVector", "Whether to scale the vector to unit length, default is true.")
  setDefault(normalizeVector, true)
  def setNormalizeVector(value: Boolean): this.type = set(normalizeVector, value)

  final val featureLabelIndex: Param[Option[Array[String]]] = new Param[Option[Array[String]]](this, "featureIndices", "")
  setDefault(featureLabelIndex, None)
  final def getOptFeatureLabelIndex: Option[Array[String]] = $(featureLabelIndex)
  def setFeatureLabelIndex(value: Array[String]): this.type = set(featureLabelIndex, Some(value))

  final val outputCol: Param[String] = new Param[String](this, "outputCol", "column name for the created vector")
  setDefault(outputCol, "vector")
  final def getOutputCol: String = $(outputCol)
  def setOutputCol(value: String): this.type = set(outputCol, value)

  final val keepOtherCols: Param[Boolean] = new Param[Boolean](this, "keepOtherCols", "keepOtherCols")
  setDefault(keepOtherCols, true)
  final def getKeepOtherCols: Boolean = $(keepOtherCols)
  def setKeepOtherCols(value: Boolean): this.type = set(keepOtherCols, value)

}



class WeightedStringGroupingVectorizer(override val uid: String)
  extends Estimator[WeightedStringGroupingVectorizerModel] with WeightedStringGroupingVectorizerParams with DefaultParamsWritable {

  def this() = this(Identifiable.randomUID("rowVecAssembler"))

  override def copy(extra: ParamMap): WeightedStringGroupingVectorizer = defaultCopy(extra)

  override def fit(dataset: Dataset[_]): WeightedStringGroupingVectorizerModel = {
    transformSchema(dataset.schema, logging = true)
    

    val featEnc = new WeightedOneHotEncoder()
      .setInputCol($(featureIdxCol))
      .setOutputCol($(outputCol))
      .setWeightCol($(weightCol))
      .setDropLast(false)

    dataset.sqlContext.udf.register("VEC_SUM", new VectorSumUDAF)
    dataset.sqlContext.udf.register("NORM_VEC_SUM", new NormalizedVectorSumUDAF)

    val usedColNames = List($(outputCol), $(groupByCol))
    val allColNames = dataset.schema.fields.map(_.name)
    val otherColNames = allColNames.filterNot(usedColNames contains _)

    val firstOfOtherCols = otherColNames.map(c => s"FIRST($c) AS $c").mkString(", ")
    val vecSumFunc = if ($(normalizeVector)) "NORM_VEC_SUM" else "VEC_SUM"
    val sqlTrans = new SQLTransformer().setStatement( // FIXME following crashes for deps features
      s"""SELECT ${$(groupByCol)},
         | ${if ($(keepOtherCols)) s"$firstOfOtherCols," else ""}
         | $vecSumFunc(${$(outputCol)}) AS ${$(outputCol)}
         | FROM __THIS__
         | GROUP BY ${$(groupByCol)}
       """.stripMargin
    )

    val stages = Array(featEnc, sqlTrans)
    val pipeline = new Pipeline().setStages(stages)
    val model = pipeline.fit(dataset)
    new WeightedStringGroupingVectorizerModel(model)
  }

  @DeveloperApi
  override def transformSchema(schema: StructType): StructType = {
    SchemaUtils.checkColumnType(schema, $(featureIdxCol), DoubleType)
    SchemaUtils.checkColumnType(schema, $(weightCol), DoubleType)
    require(schema.fieldNames.contains($(groupByCol)), s"The groupByCol ${$(groupByCol)} does not exist.")
    if (schema.fieldNames.contains($(outputCol))) {
      throw new IllegalArgumentException(s"Output column ${$(outputCol)} already exists.")
    }

    val outputFields = schema.fields :+
      StructField($(outputCol), VectorType , nullable = false)
    StructType(outputFields)
  }
}

/**
  * A model that is used as a transformer to create word vectors. The input dataset is expected
  * to have one grouping column and another column with string features, additionally a weight
  * column is expected.
  * The transformer first vectorizes all string features and than groups all rows accordingly to
  * the grouping column and calucaltes their weighted sum.
  */

class WeightedStringGroupingVectorizerModel(override val uid: String, pipeline: PipelineModel)
  extends Model[WeightedStringGroupingVectorizerModel] {

  def this(pipeline: PipelineModel) =
    this(Identifiable.randomUID("weightedStringGroupingVectorizerModel"), pipeline)

  override def copy(extra: ParamMap): WeightedStringGroupingVectorizerModel = defaultCopy(extra)

  override def transform(dataset: Dataset[_]): DataFrame = pipeline.transform(dataset)

  override def transformSchema(schema: StructType): StructType = pipeline.transformSchema(schema)

}