package de.tudarmstadt.lt.wsd.pipeline.sql

import org.apache.spark.ml.linalg.{LinalgShim, Vector, Vectors}
import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
import org.apache.spark.sql.Row
import org.apache.spark.sql.expressions.{MutableAggregationBuffer, UserDefinedAggregateFunction}
import org.apache.spark.sql.types.{StructField, _}

// See http://stackoverflow.com/a/33901072
// For a blueprint and comparison between DataFrames agg and using RDDs foldByKey

// See https://ragrawal.wordpress.com/2015/11/03/spark-custom-udaf-example/
// For general overview of UDAFs in spark

// See http://stackoverflow.com/a/39084121
// for using VectorType instead of VectorUDF


class VectorSumUDAF extends UserDefinedAggregateFunction {
  def inputSchema = new StructType().add("v", VectorType)

  def bufferSchema = new StructType().add("buff", StructType(Seq(
    StructField("map", MapType(IntegerType, DoubleType)),
    StructField("size", IntegerType)
  )))

  def dataType = VectorType

  def deterministic = true

  def initialize(buffer: MutableAggregationBuffer) = {
    val unknownSize = -1
    buffer.update(0, Row(Map(), unknownSize))
  }

  def update(buffer: MutableAggregationBuffer, input: Row) = {
    if (!input.isNullAt(0)) {
      val buff = buffer.getAs[Row](0)
      val map1 = buff.getAs[Map[Int, Double]]("map")

      val vector = input.getAs[Vector](0).toSparse
      val map2 = vector.indices.zip(vector.values).toMap
      
      val map = VectorSumUDAF.mergeMaps(map1, map2)

      buffer.update(0, Row(map, vector.size))
    }
  }

  def merge(buffer1: MutableAggregationBuffer, buffer2: Row) = {
    var buff1 = buffer1.getAs[Row](0)
    val buff2 = buffer2.getAs[Row](0)
    val n1 = buff1.getAs[Int]("size")
    val n2 = buff2.getAs[Int]("size")
    val map1 = buff1.getAs[Map[Int, Double]]("map")
    val map2 = buff2.getAs[Map[Int, Double]]("map")
    val n = if (n1 > n2) n1 else n2
    val map = VectorSumUDAF.mergeMaps(map1, map2)
    buffer1.update(0, Row(map, n))
  }

  def evaluate(buffer: Row) = {
    val buff = buffer.getAs[Row](0)
    val n = buff.getAs[Int]("size")
    val map = buff.getAs[Map[Int, Double]]("map")
    Vectors.sparse(n, map.toSeq)
  }
}

object VectorSumUDAF {
  // Merge by adding values of same keys
  def mergeMaps(map1: Map[Int, Double], map2: Map[Int, Double]): Map[Int, Double] = {
    map1.foldLeft(map2) {
      case (to, (k, v)) => to + (k -> to.get(k).map(_ + v).getOrElse(v))
    }
  }
}

class NormalizedVectorSumUDAF extends VectorSumUDAF {
  override def evaluate(buffer: Row): Vector = {
    val vec = super.evaluate(buffer)
    LinalgShim.normalize(vec)
    vec
  }
}