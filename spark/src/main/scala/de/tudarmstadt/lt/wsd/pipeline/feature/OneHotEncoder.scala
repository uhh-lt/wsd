package de.tudarmstadt.lt.wsd.pipeline.feature

import org.apache.spark.ml.attribute._
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.ml.param.Param
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.{OneHotEncoder => SparkOneHotEncoder}
import org.apache.spark.sql.types.DoubleType
import org.apache.spark.sql.{DataFrame, Dataset}


// Mostly copied from SparkOneHotEncoder, only difference is that array of labels for sparse vector are not saved
// in metadata, because it this causes memory issues for large dimensions
class OneHotEncoder(override val uid: String) extends SparkOneHotEncoder {

  def this() = this(Identifiable.randomUID("fixedOneHot"))


  override def transform(dataset: Dataset[_]): DataFrame = {
    // schema transformation
    val inputColName = $(inputCol)
    val outputColName = $(outputCol)
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

      // Removing Array.tabulates basically creates an array with numAttrs, if numAttrs is big, this needs a lot memory
      // REMOVED: val outputAttrNames = Array.tabulate(numAttrs)(_.toString)
      // Plus a few other lines...

      val size = if (shouldDropLast) numAttrs -1 else numAttrs
      outputAttrGroup = new AttributeGroup(outputColName, size)
    }
    val metadata = outputAttrGroup.toMetadata()

    // data transformation
    val size = outputAttrGroup.size
    val oneValue = Array(1.0)
    val emptyValues = Array.empty[Double]
    val emptyIndices = Array.empty[Int]
    val encode = udf { label: Double =>
      if (label < size) {
        Vectors.sparse(size, Array(label.toInt), oneValue)
      } else {
        Vectors.sparse(size, emptyIndices, emptyValues)
      }
    }

    dataset.select(col("*"), encode(col(inputColName).cast(DoubleType)).as(outputColName, metadata))
  }

}
