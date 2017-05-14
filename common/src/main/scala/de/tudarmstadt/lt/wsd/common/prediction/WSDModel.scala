package de.tudarmstadt.lt.wsd.common.prediction

import de.tudarmstadt.lt.wsd.common.model.{Sense, SenseVectorModel, WordVector}

/**
  * Created by fide on 12.04.17.
  */
case class WSDModel(
  classifier: WordSenseClassifiers.Classifier,
  sense_inventory: Sense.InventoryName,
  word_vector_model: WordVector.ModelName) {

  val sense_vector_model = SenseVectorModel(
    sense_inventory = sense_inventory,
    word_vector_model = word_vector_model
  )
  override def toString: String = s"${classifier}_${sense_inventory}_$word_vector_model"

  // TODO maybe refactor this?! There should be a possiblity to have
  // TODO multiple inventories of the same type without creating a specific enum.
  // TODO maybe a configuation table would be appropiate with a name column
  def isInventoryCoSet: Boolean = Sense.isInventoryCoset(sense_inventory)

  def apply(name: String): WSDModel = WSDModel.parseFromString(name)
}

object WSDModel {
  def parseFromString(model: String): WSDModel = {

    model.split("_") match {
      case Array(classifier: String) =>
        WSDModel(
          classifier = WordSenseClassifiers withName classifier,
          sense_inventory = Sense.any,
          word_vector_model = WordVector.any // for random and other baselines
        )
      case Array(classifier: String, inventory: String) =>
        WSDModel(
          classifier = WordSenseClassifiers withName classifier,
          sense_inventory = Sense withName inventory,
          word_vector_model = WordVector.any // for random and other baselines
        )
      case Array(classifier: String, inventory: String, word_vector_model: String) =>
        WSDModel(
          classifier = WordSenseClassifiers withName classifier,
          sense_inventory = Sense withName inventory,
          word_vector_model = WordVector withName word_vector_model
        )
    }
  }
}
