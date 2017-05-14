package de.tudarmstadt.lt.wsd.common.prediction
import breeze.linalg.{Vector => BV}
import Math.log

import com.typesafe.scalalogging.LazyLogging
/**
  * Created by fide on 14.04.17.
  */

object VectorDistanceMeasure extends LazyLogging {
  type VectorDistanceMeasure = (BV[Double], BV[Double]) => Double
  val cosine: VectorDistanceMeasure = (v1: BV[Double], v2: BV[Double]) => v1.dot(v2)
  val random: VectorDistanceMeasure = (v1: BV[Double], v2: BV[Double]) => scala.util.Random.nextDouble()

  val naivebayes: (Double) => VectorDistanceMeasure = (priorV1: Double) => (v1: BV[Double], v2: BV[Double]) => {
    val pairwise = hadamardProduct(v1, v2)
    val numMissing = v1.activeSize - pairwise.activeSize

    val existingLogs = pairwise.activeValuesIterator.toList.map(log)

    existingLogs.fold(0.0)(_ + _) + (log(priorV1) * numMissing)
  }

  def hadamardProduct: (BV[Double], BV[Double]) => BV[Double] = (v1: BV[Double], v2: BV[Double]) => v1 :* v2
}
