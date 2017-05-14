package de.tudarmstadt.lt.wsd.common.prediction
import de.tudarmstadt.lt.wsd.common.model.{SenseInventory, SenseVector, SenseVectorModel}
import breeze.linalg.{Vector => BV}
import com.typesafe.scalalogging.LazyLogging
import de.tudarmstadt.lt.wsd.common.prediction.VectorDistanceMeasure.VectorDistanceMeasure

/**
  * Created by fide on 14.04.17.
  */

object WordSenseClassifiers extends Enumeration {
  type Classifier = Value
  val cos, random, naivebayes, ensemble = Value

  def getGenerator(model: WSDModel): (String) => WordSenseClassifier = {

    val inventory: SenseInventory = SenseInventory.getInstance(model.sense_vector_model)

    model.classifier match {

      case `cos` =>
        (word: String) => new WordSpaceCosineClassifier(word, inventory.getSenses(word))

      case `random` =>
        (word: String) => new RandomBaselineClassifier(word, inventory.getSenses(word))

      case `naivebayes` =>
        (word: String) => new NaiveBayesClassifier(word, inventory.getSenses(word))

      case `ensemble` =>
        throw new IllegalArgumentException(
          "The ensemble classifier must use the EnsemblePredictionPipeline")
    }
  }
}

abstract class WordSenseClassifier {

  def classify(contextVector: BV[Double]): Seq[Prediction]
}

case class Prediction(sense: SenseVector, score: Double)

class DistanceBasedWSClassifier[M <: VectorDistanceMeasure](word: String, senses: Seq[SenseVector], measure: VectorDistanceMeasure)
  extends WordSenseClassifier with LazyLogging {

  override def classify(contextVector: BV[Double]): Seq[Prediction] =
    senses.map{s =>
      Prediction(
        sense = s,
        score = measure(contextVector, s.vector))
    }.sortBy(- _.score) // Minus for inverting sort
}

class WordSpaceCosineClassifier(word: String, senses: Seq[SenseVector])
  extends DistanceBasedWSClassifier(word, senses, VectorDistanceMeasure.cosine)

class RandomBaselineClassifier(word: String, senses: Seq[SenseVector])
  extends DistanceBasedWSClassifier(word, senses, VectorDistanceMeasure.random)


class NaiveBayesClassifier(word: String, senses: Seq[SenseVector])
  extends DistanceBasedWSClassifier(word, senses, VectorDistanceMeasure.naivebayes(0.00001))