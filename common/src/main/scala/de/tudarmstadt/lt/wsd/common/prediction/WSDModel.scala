package de.tudarmstadt.lt.wsd.common.prediction

import de.tudarmstadt.lt.wsd.common.model.{Sense, SenseInventory, SenseVectorModel, WordVector}

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

  override def toString: String = {
    val inventory_component = if (sense_inventory == Sense.undefined) "" else s"_$sense_inventory"
    val word_vector_component = if (word_vector_model == WordVector.undefined) "" else s"_$word_vector_model"
    s"$classifier$inventory_component$word_vector_component"
  }

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
        assert(classifier == WordSenseClassifiers.ensemble.toString,
          "Currently only ensemble can omit inventory and word vector components")
        WSDModel(
          classifier = WordSenseClassifiers withName classifier,
          sense_inventory = Sense.undefined,
          word_vector_model = WordVector.undefined
        )
      case Array(classifier: String, inventory: String) =>
        WSDModel(
          classifier = WordSenseClassifiers withName classifier,
          sense_inventory = Sense withName inventory,
          word_vector_model = WordVector.undefined // for random and other baselines
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
