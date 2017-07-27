package de.tudarmstadt.lt.wsd.common.prediction
import breeze.linalg.{Vector => BV}
import Math.{log, exp}

import com.typesafe.scalalogging.slf4j.LazyLogging
/**
  * Created by fide on 14.04.17.
  */

object VectorDistanceMeasure extends LazyLogging {
  type VectorDistanceMeasure = (BV[Double], BV[Double]) => Double
  val cosine: VectorDistanceMeasure = (v1: BV[Double], v2: BV[Double]) => v1.dot(v2)
  val random: VectorDistanceMeasure = (v1: BV[Double], v2: BV[Double]) => scala.util.Random.nextDouble()

  val naivebayes: (Double) => VectorDistanceMeasure = (priorV1: Double) => (v1: BV[Double], v2: BV[Double]) => {
    if (v1.activeSize == 0)
      0 // If v1 is empty (i.e. context was empty or all out-off-voc-words), the result would be exp(0) = 1, better to return 0 then
    else {
      val pairwise = hadamardProduct(v1, v2)
      val numMissing = v1.activeSize - pairwise.activeSize

      val existingLogs = pairwise.activeValuesIterator.toList.map(log)

      exp(existingLogs.fold(0.0)(_ + _) + (log(priorV1) * numMissing))
    }
  }

  def hadamardProduct: (BV[Double], BV[Double]) => BV[Double] = (v1: BV[Double], v2: BV[Double]) => v1 :* v2
}
