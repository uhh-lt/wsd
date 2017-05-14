package de.tudarmstadt.lt.wsd.common.utils

import breeze.linalg.SparseVector
import breeze.linalg.CSCMatrix

/**
  * Created by fide on 14.12.16.
  */
object BreezeUtils {

  def toCSCMatrix(columnVectors: List[SparseVector[Double]])  = {
    val vecLength = columnVectors.head.length
    val numVectors = columnVectors.length

    val builder = new CSCMatrix.Builder[Double](rows=vecLength, cols=numVectors)

    for ((vector, col) <- columnVectors.zipWithIndex) {
      for ((row, value) <- vector.activeIterator) {
        builder.add(row, col, value)
      }
    }

    builder.result(keysAlreadyUnique = true, keysAlreadySorted = true)
  }

  def toCSCRowVector(rowVector: SparseVector[Double]) = {

  }
}
