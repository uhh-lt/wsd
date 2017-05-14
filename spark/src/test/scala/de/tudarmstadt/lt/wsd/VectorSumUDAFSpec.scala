package de.tudarmstadt.lt.wsd

import de.tudarmstadt.lt.wsd.pipeline.utils.RuntimeContext
import org.scalatest.{FlatSpec, Ignore}
import org.scalatest.Matchers._
import org.apache.spark.ml.linalg.{LinalgShim, SparseVector, Vectors}
import de.tudarmstadt.lt.wsd.pipeline.sql.{NormalizedVectorSumUDAF, VectorSumUDAF}

class VectorSumUDAFSpec extends FlatSpec with RuntimeContext {
  import spark.implicits._

  "Summed up vectors" should "have correct length" in {
    val vec1 = Vectors.sparse(1, Seq((0, 2.0)))
    val vec2 = Vectors.sparse(2, Seq((1, 2.0)))
    val vec3 = Vectors.sparse(2, Seq((1, 1.0)))
    val df = Seq(Some(vec1), Some(vec2), Some(vec3)).toDF("vec")

    spark.sqlContext.udf.register("VEC_SUM", new VectorSumUDAF)
    df.createOrReplaceTempView("vectors")

    val summedVec = spark.sql("SELECT VEC_SUM(vec) FROM vectors").first.getAs[SparseVector](0)

    LinalgShim.vectorLength(summedVec) should equal (5.0)
  }

  "Norm summed vectors" should "be normalized" in {
    val vec1 = Vectors.sparse(1, Seq((0, 2.0)))
    val vec2 = Vectors.sparse(2, Seq((1, 2.0)))
    val vec3 = Vectors.sparse(2, Seq((1, 1.0)))
    val df = Seq(Some(vec1), Some(vec2), Some(vec3)).toDF("vec")

    spark.sqlContext.udf.register("NORM_VEC_SUM", new NormalizedVectorSumUDAF)
    df.createOrReplaceTempView("vectors")

    val summedVec = spark.sql("SELECT NORM_VEC_SUM(vec) FROM vectors")
      .first.getAs[SparseVector](0)

    LinalgShim.vectorLength(summedVec) should equal (1.0)
  }


}

