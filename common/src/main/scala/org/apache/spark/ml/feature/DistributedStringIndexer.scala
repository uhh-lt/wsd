package org.apache.spark.ml.feature

import org.apache.hadoop.fs.Path
import org.apache.spark.annotation.Since
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.ml.attribute.NominalAttribute
import org.apache.spark.ml.param._
import org.apache.spark.ml.util._
import org.apache.spark.ml.{Estimator, Model}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Dataset, Row}
import org.apache.spark.util.collection.OpenHashMap
import org.apache.spark.{SparkContext, SparkException}

trait DistributedStringIndexerBase extends Params {

  final val inputCol: Param[String] = new Param[String](this, "inputCol", "")
  final def getInputCol: String = $(inputCol)
  def setInputCol(value: String): this.type = set(inputCol, value)
  
  final val outputCol: Param[String] = new Param[String](this, "outputCol", "")
  final def getOutputCol: String = $(outputCol)
  def setOutputCol(value: String): this.type = set(outputCol, value)

  protected def validateAndTransformSchema(schema: StructType): StructType = {
    val inputColName = $(inputCol)
    val inputDataType = schema(inputColName).dataType
    require(inputDataType == StringType || inputDataType.isInstanceOf[NumericType],
      s"The input column $inputColName must be either string type or numeric type, " +
        s"but got $inputDataType.")
    val inputFields = schema.fields
    val outputColName = $(outputCol)
    require(inputFields.forall(_.name != outputColName),
      s"Output column $outputColName already exists.")

    val attr = NominalAttribute.defaultAttr.withName($(outputCol))
    val outputFields = inputFields :+ attr.toStructField()
    StructType(outputFields)
  }
}


class DistributedStringIndexer (override val uid: String) extends Estimator[DistributedStringIndexerModel]
  with DistributedStringIndexerBase with DefaultParamsWritable {

  def this() = this(Identifiable.randomUID("strIdx"))

  override def fit(dataset: Dataset[_]): DistributedStringIndexerModel = {
    transformSchema(dataset.schema, logging = true)
    import dataset.sparkSession.implicits._

    val labels = dataset.select(col($(inputCol)).cast(StringType))
      .rdd
      .map{ case Row(string: String) => (string, 1L) }
      .reduceByKey(_ + _)
      .sortBy(-_._2)
      .zipWithIndex()
      .map{case ((string, _), index) => (string, index.toDouble)}.toDS

    copyValues(new DistributedStringIndexerModel(uid, labels).setParent(this))
  }

  override def transformSchema(schema: StructType): StructType = {
    validateAndTransformSchema(schema)
  }

  override def copy(extra: ParamMap): DistributedStringIndexer = defaultCopy(extra)
}


class DistributedStringIndexerModel(
  override val uid: String,
  val labels: Dataset[(String, Double)])
  extends Model[DistributedStringIndexerModel] with DistributedStringIndexerBase {

  def this(labels: Dataset[(String, Double)]) = this(Identifiable.randomUID("distrStrIdx"), labels)

  override def transform(dataset: Dataset[_]): DataFrame = {
    if (!dataset.schema.fieldNames.contains($(inputCol))) {
      logInfo(s"Input column ${$(inputCol)} does not exist during transformation. " +
        "Skip StringIndexerModel.")
      return dataset.toDF
    }
    import dataset.sparkSession.implicits._
    transformSchema(dataset.schema, logging = true)

    dataset.as("ds")
      .join(labels.toDF("label", "index").as("idxr"))
      .where(col($(inputCol)) === $"idxr.label")
      .withColumn($(outputCol), $"idxr.index")
      .select("ds.*", $(outputCol))

  }


  override def transformSchema(schema: StructType): StructType = {
    if (schema.fieldNames.contains($(inputCol))) {
      validateAndTransformSchema(schema)
    } else {
      // If the input column does not exist during transformation, we skip StringIndexerModel.
      schema
    }
  }

  override def copy(extra: ParamMap): DistributedStringIndexerModel = {
    val copied = new DistributedStringIndexerModel(uid, labels)
    copyValues(copied, extra).setParent(parent)
  }
}


object DistributedStringIndexerModel extends MLReadable[DistributedStringIndexerModel] {

  private[DistributedStringIndexerModel]
  class StringIndexModelWriter(instance: DistributedStringIndexerModel) extends MLWriter {

    override protected def saveImpl(path: String): Unit = {
      DefaultParamsWriter.saveMetadata(instance, path, sc)
      val dataPath = new Path(path, "data").toString
      instance.labels.write.parquet(dataPath)
    }
  }

  private class DistributedStringIndexerModelReader extends MLReader[DistributedStringIndexerModel] {

    private val className = classOf[DistributedStringIndexerModel].getName

    override def load(path: String): DistributedStringIndexerModel = {
      val metadata = DefaultParamsReader.loadMetadata(path, sc, className)
      val dataPath = new Path(path, "data").toString
      val labels = sparkSession.read.parquet(dataPath)
      import labels.sparkSession.implicits._
      val model = new DistributedStringIndexerModel(metadata.uid, labels.as[(String, Double)])
      DefaultParamsReader.getAndSetParams(model, metadata)
      model
    }
  }

  override def read: MLReader[DistributedStringIndexerModel] = new DistributedStringIndexerModelReader
  override def load(path: String): DistributedStringIndexerModel = super.load(path)
}