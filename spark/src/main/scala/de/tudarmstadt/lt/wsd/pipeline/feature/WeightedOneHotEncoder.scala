package de.tudarmstadt.lt.wsd.pipeline.feature

import org.apache.spark.ml.attribute._
import org.apache.spark.ml.feature.OneHotEncoder
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.ml.param.Param
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{DoubleType, NumericType, StructType}

/**
  * Created by fide on 03.09.16.
  */
class WeightedOneHotEncoder(override val uid: String) extends OneHotEncoder {

  def this() = this(Identifiable.randomUID("weightHot"))

  final val weightCol: Param[String] = new Param[String](this, "weightCol", "weight column name")

  def setWeightCol(value: String): this.type = set(weightCol, value)

  override def transform(dataset: Dataset[_]): DataFrame = {
    // schema transformation
    val inputColName = $(inputCol)
    val outputColName = $(outputCol)
    val weightColName = $(weightCol)
    val shouldDropLast = $(dropLast)
    var outputAttrGroup = AttributeGroup.fromStructField(
      transformSchema(dataset.schema)(outputColName))
    if (outputAttrGroup.size < 0) {
      // If the number of attributes is unknown, we check the values from the input column.
      val numAttrs = dataset.select(col(inputColName).cast(DoubleType)).rdd.map(_.getDouble(0))
        .aggregate(0.0)(
          (m, x) => {
            assert(x <= Int.MaxValue,
              s"OneHotEncoder only supports up to ${Int.MaxValue} indices, but got $x")
            assert(x >= 0.0 && x == x.toInt,
              s"Values from column $inputColName must be indices, but got $x.")
            math.max(m, x)
          },
          (m0, m1) => {
            math.max(m0, m1)
          }
        ).toInt + 1
      outputAttrGroup = new AttributeGroup(outputColName, numAttrs)
    }
    val metadata = outputAttrGroup.toMetadata()

    // data transformation
    val size = outputAttrGroup.size
    val emptyValues = Array[Double]()
    val emptyIndices = Array[Int]()
    val encode = udf { (label: Double, weight: Double) =>
      if (label < size) {
        Vectors.sparse(size, Array(label.toInt), Array(weight))
      } else {
        Vectors.sparse(size, emptyIndices, emptyValues)
      }
    }

    dataset.select(
      col("*"),
      encode(col(inputColName).cast(DoubleType), col(weightColName).cast(DoubleType)).as(outputColName, metadata))
  }

}
