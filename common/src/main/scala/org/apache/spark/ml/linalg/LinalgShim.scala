package org.apache.spark.ml.linalg
import breeze.linalg.{Vector => BV}

/**
  * Created by fide on 25.11.16.
  * Copied from https://github.com/karlhigley/spark-neighbors
  */
object LinalgShim {

  /**
    * Compute the dot product between two vectors
    *
    * Under the hood, Spark's BLAS module calls a BLAS routine
    * from netlib-java for the case of two dense vectors, or an
    * optimized Scala implementation in the case of sparse vectors.
    */
  def dot(x: Vector, y: Vector): Double = {
    BLAS.dot(x, y)
  }

  def vectorLength(x: Vector): Double = x match {
    case sx: SparseVector => sx.values.sum
    case dx: DenseVector => dx.values.sum
    case _ =>
      throw new IllegalArgumentException(s"Vector type ${x.getClass} not supported.")
  }

  // actually only a valid norm for non-negative components of the vector
  // http://stackoverflow.com/a/11225139
  def normalize(x: Vector): Vector = {
    val sum: Double = vectorLength(x)
    val factor = 1.0 / sum
    scale(factor, x)
  }



  def scale(a: Double, x: Vector) = {
    BLAS.scal(a, x)
    x // Because BLAS does not return the vector
  }

  def add(x: Vector, y: Vector) = {
    fromBreeze(asBreeze(x) + asBreeze(x))
  }

  /**
    * Convert a Spark vector to a Breeze vector to access
    * vector operations that Spark doesn't provide.
    */
  def asBreeze(x: Vector): BV[Double] = {
    x.asBreeze
  }

  def fromBreeze(x: BV[Double]): Vector = {
    Vectors.fromBreeze(x)
  }
}