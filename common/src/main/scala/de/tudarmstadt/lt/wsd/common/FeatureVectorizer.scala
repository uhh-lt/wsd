package de.tudarmstadt.lt.wsd.common

import breeze.linalg.{SparseVector, Vector => BV}
import com.typesafe.scalalogging.LazyLogging
import de.tudarmstadt.lt.wsd.common.model.{IndexedFeature, WordVector}

object FeatureVectorizer {
  var vectorizerMap: Map[WordVector.ModelName, FeatureVectorizer] =
    Map.empty[WordVector.ModelName, FeatureVectorizer]

  def getVectorizer(wordVectorModel: WordVector.ModelName): FeatureVectorizer = {
    vectorizerMap.get(wordVectorModel) match {
      case Some(vectorizer) => vectorizer
      case None =>
          val labels = IndexedFeature.findAllByModelSorted(wordVectorModel).map(_.label).toArray
          val vectorizer = new FeatureVectorizer(labels)
          vectorizerMap += (wordVectorModel -> vectorizer)
          vectorizer
    }
  }
}

class FeatureVectorizer(val labels: Array[String]) extends LazyLogging  {

  val indexMap: Map[String, Int] = labels.zipWithIndex.toMap
  val vecSize: Int = labels.length

  def indexFeature(word: String): Option[Int] = {
    try {
      Some(indexMap(word))
    } catch {
      case e: java.util.NoSuchElementException => None
    }
  }

  def doVectorize(features: Array[Int]): BV[Double] = {
    val l = features.length
    new SparseVector[Double](features, Array.fill(l)(1.0 / l), vecSize)
  }

  def doVectorize(features: Map[Int, Double]): BV[Double] = {
    new SparseVector[Double](features.keys.toArray, features.values.toArray, vecSize)
  }

  def doVectorize(features: Seq[String]): BV[Double] = {
    val indexedFeatures = features.toSet.toArray.flatMap(f => indexFeature(f)).sorted
    doVectorize(indexedFeatures)
  }

  def doUnvectorize(vector: BV[Double]): Seq[Feature] = {
    vector.activeIterator.toArray.map{case (idx: Int, value: Double) => Feature(labels(idx), value)}
  }

}

case class Feature(label: String, weight: Double)

