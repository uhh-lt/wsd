package de.tudarmstadt.lt.wsd.common

import org.scalatest.FlatSpec
import breeze.linalg.{SparseVector => BSV}
import de.tudarmstadt.lt.wsd.common.prediction.VectorDistanceMeasure
import org.scalatest.Matchers._

class VectorDistanceMeasureSpec extends FlatSpec {

  "Naive bayes" should "should give vectors with many matching components higher score" in {
    val v1Prior = 0.001
    val naivebayes = VectorDistanceMeasure.naivebayes(v1Prior)

    // Reference vector v1 = (1,1,1,1), to calculate similarity with
    val v1 = new BSV[Double](Array(0,1,2,3), Array(1,1,1,1), 4)

    // Only one matching component, with low score against v2:
    // -17.727 = log(20) + log(prior) + log(prior) + log(prior)
    val v2oneMatch = new BSV[Double](Array(0), Array(20), 4)

    // Four matching components, with high score against v2:
    // 2.772  = log(2) + log(2) + log(2) + log(2)
    val v2fourMatches = new BSV[Double](Array(0,1,2,3), Array(2,2,2,2), 4)

    naivebayes(v1, v2fourMatches) should be > naivebayes(v1, v2oneMatch)
  }

}

