package de.tudarmstadt.lt.wsd.common.eval

import com.typesafe.scalalogging.slf4j.LazyLogging
import de.tudarmstadt.lt.wsd.common.utils.{ScalaNLPUtils, TSVUtils, Utils}
import de.tudarmstadt.lt.wsd.common.model.{Sense => InternalSense}
import de.tudarmstadt.lt.wsd.common.prediction.WSDModel

/**
  * Created by fide on 11.01.17.
  */

object SenseInventoryMapping extends LazyLogging {
  def loadFromTSVForModel(path: String, modelConfig: WSDModel) = {
    val inventory = readSensesFromTSV(path)
    create(inventory, modelConfig)
  }

  def loadFromTSVFiles(internalPath: String, externalPath: String, internalIsGlobal: Boolean): AbstractInventoryMapping = {
    val internalInventory = readSensesFromTSV(internalPath)
    val externalInventory = readSensesFromTSV(externalPath)
    create(internalInventory, externalInventory, internalIsGlobal)
  }

  def create(internalInventory: List[Sense], externalInventory: List[Sense], internalIsGlobal: Boolean): AbstractInventoryMapping = {
    if (internalIsGlobal)
      new CoSetInventoryMapping(internalInventory, externalInventory)
    else
      new SenseInventoryMapping(internalInventory, externalInventory)
  }

  def create(externalInventory: List[Sense], modelConfig: WSDModel): AbstractInventoryMapping = {
    val internalInventory = InternalSense.findAll().map(
      i => Sense(id = i.sense_id, word = i.word, hypernyms = i.hypernyms.toList)
    )
    create(internalInventory, externalInventory, modelConfig.isInventoryCoSet)
  }

  def readSensesFromTSV(path: String) = {
    val (_, rows) = TSVUtils.readWithHeaders(path)
    val inventory = rows.map { r =>
      Sense(id = r("id"), word = r("word"), hypernyms = r("related_words").split(",").toList)
    }
    inventory.toList
  }
}

abstract class AbstractInventoryMapping {

  val internalInventoryByID: Map[String, Sense]
  val externalInventoryByID: Map[String, Sense]
  def filterUnmappedInternalIDsForWord(internalIDs: List[String], externalID: String): List[String]

  def isExternalMapped(externalID: String): Boolean
  def isInternalMapped(internalID: String): Boolean

  def numMappingsForInternal(internalID: String): Int
  def numMappingsForExternal(externalID: String): Int

  def areIDsMapped(internalID: String, externalID: String): Boolean

  def statsList() : List[(String, String)]
}

class SenseInventoryMapping(internalInventory: List[Sense], externalInventory: List[Sense]) extends AbstractInventoryMapping with LazyLogging {

  val internalInventoryByID = internalInventory.map(s => s.id -> s).toMap
  val externalInventoryByID = externalInventory.map(t => t.id -> t).toMap

  val groupIDsByWord = (inventory: List[Sense]) =>
      inventory.groupBy(_.word.toLowerCase()).mapValues(s => s.map(_.id)).withDefaultValue(List())

  val externalIDsByWord = groupIDsByWord(externalInventory)
  val internalIDsByWord = groupIDsByWord(internalInventory)

  val pairIDs = externalIDsByWord.toList.flatMap{case (word, externalIDs) =>
      val internalIDs = internalIDsByWord(word) // This can handle the case noInternalWordScope, because internalIDsByWord is withDefaultValue
      externalIDs.flatMap(e => internalIDs.map(i => (i, e)))
    }

  val mappings = pairIDs.map{ case (i, e) =>
    SenseMapping.create(internalInventoryByID(i), externalInventoryByID(e))
  }

  val mappingsByID = mappings.map(m => m.id -> m).toMap

  val mappingIDsByInternalID = mappings.groupBy(_.internalID).mapValues(ms => ms.map(_.id))
  val mappingIDsByExternalID = mappings.groupBy(_.externalID).mapValues(ms => ms.map(_.id))

  // Sense querying methods

  def isInternalMapped(internalID: String) = {
    internalInventoryByID(internalID) // Throw exception if it does not exist
    mappingIDsByInternalID.getOrElse(internalID, Nil).map(mappingsByID).exists(_.isMapped)
  }

  def isExternalMapped(externalID: String) = {
    externalInventoryByID(externalID) // Throw exception if it does not exist
    mappingIDsByExternalID.getOrElse(externalID, Nil).map(mappingsByID).exists(_.isMapped)
  }


  override def numMappingsForInternal(internalID: String): Int = {
    internalInventoryByID(internalID) // Throw exception if it does not exist
    mappingIDsByInternalID.getOrElse(internalID, Nil).map(mappingsByID).count(_.isMapped)
  }

  override def numMappingsForExternal(externalID: String): Int = {
    externalInventoryByID(externalID) // Throw exception if it does not exist
    mappingIDsByExternalID.getOrElse(externalID, Nil).map(mappingsByID).count(_.isMapped)
  }

  override def filterUnmappedInternalIDsForWord(internalIDs: List[String], word: String) = {
    if (internalIDs.toSet != internalIDsByWord(word).toSet) {
      logger.error(s"For word $word, ${internalIDs.toSet} does not equal ${internalIDsByWord(word).toSet}!")
    }
    internalIDs.filter(isInternalMapped)
  }

  override def areIDsMapped(internalID: String, externalID: String) = {
    mappingsByID(s"$internalID->$externalID").isMapped
  }

  def internalMappingProjection = {
    internalInventoryByID.keys.map(i =>
      Map(
        "internal_id" -> i,
        "external_ids" -> mappingIDsByInternalID.getOrElse(i, Nil).map(mappingsByID).map(_.externalID).mkString(",")
      )
    )
  }

  // Stats methods

  // How many internals are mapped to an external
  def countMappedInternals() = {
    internalInventoryByID.keys.count(isInternalMapped)
  }

  def countMappedExternals() = {
    externalInventoryByID.keys.count(isExternalMapped)
  }

  def avgInternalsPerWord(excludeZero: Boolean = true) = {
    Utils.avg(internalIDsByWord.values.map(_.length).toList, excludeZero)
  }

  def avgExternalsPerWord(excludeZero: Boolean = true) = {
    Utils.avg(externalIDsByWord.values.map(_.length).toList, excludeZero)
  }

  // On average how many internals exist per word, which have been mapped
  def avgMappedInternalsPerWord(excludeZero: Boolean = true) = {
    Utils.avg(internalIDsByWord.values.map{_.count(isInternalMapped)}.toList, excludeZero)
  }

  def avgMappedExternalsPerWord(excludeZero: Boolean = true) = {
    Utils.avg(externalIDsByWord.values.map{_.count(isExternalMapped)}.toList, excludeZero)
  }

  override def statsList() = {
    List(
      ("num internal senses", s"${internalInventoryByID.size}"),
      ("num internal senses mapped", s"${countMappedInternals()}"),
      ("avg internal senses per word (exclude zero)", s"${avgInternalsPerWord()}"),
      ("avg mapped internal senses per word (exclude zero)", s"${avgMappedInternalsPerWord()}"),
      ("----",""),
      ("num external senses", s"${externalInventoryByID.size}"),
      ("avg external senses per word (exclude zero)", s"${avgExternalsPerWord()}"),
      ("avg mapped external senses per word (exclude zero)", s"${avgMappedExternalsPerWord()}")
    )
  }

}

class CoSetInventoryMapping(internalInventory: List[Sense], externalInventory: List[Sense])  extends AbstractInventoryMapping  {

  val internalInventoryByID = internalInventory.map(s => s.id -> s).toMap // CoSets
  val externalInventoryByID = externalInventory.map(t => t.id -> t).toMap

  val groupIDsByWord = (inventory: List[Sense]) =>
    inventory.groupBy(_.word.toLowerCase()).mapValues(s => s.map(_.id)).withDefaultValue(List())

  val externalIDsByWord = groupIDsByWord(externalInventory)

  override def filterUnmappedInternalIDsForWord(internalIDs: List[String], word: String) = {
    val externalIDs = externalIDsByWord(word)
    internalIDs.filter(i => externalIDs.exists(e => areIDsMapped(i, e)))
  }

  def statsList() = {
    List(
      ("num internal senses", s"${internalInventoryByID.size}"),
      ("----",""),
      ("num external senses", s"${externalInventoryByID.size}")
    )
  }

  override def isExternalMapped(externalID: String) = {
    val external = externalInventoryByID(externalID) // Throw exception if it does not exist
    internalInventory.exists(SenseMapping.create(_, external).isMapped)
  }

  override def isInternalMapped(internalID: String) = {
    val internal = internalInventoryByID(internalID) // Throw exception if it does not exist
    externalInventory.exists(SenseMapping.create(internal, _).isMapped)
  }

  override def areIDsMapped(internalID: String, externalID: String) = {
    val external = externalInventoryByID(externalID)
    val internal = internalInventoryByID(internalID)
    SenseMapping.create(internal, external).isMapped
  }

  override def numMappingsForInternal(internalID: String): Int = {
    val internal = internalInventoryByID(internalID) // Throw exception if it does not exist
    externalInventory.count(SenseMapping.create(internal, _).isMapped)
  }

  override def numMappingsForExternal(externalID: String): Int = {
    val external = externalInventoryByID(externalID) // Throw exception if it does not exist
    internalInventory.count(SenseMapping.create(_, external).isMapped)
  }
}


object SenseMapping {
  def create(internal: Sense, external: Sense): SenseMapping = {
    val matchingHypernyms = internal.cleanedHypernyms.intersect(external.cleanedHypernyms)
    SenseMapping(internal.id, external.id, matchingHypernyms)
  }

  def cleanHypernyms(hypernyms: List[String]): List[String] = {
    val lemmas = ScalaNLPUtils.convertToLemmas(hypernyms.mkString(" ").replace('_', ' ')).map(_.toLowerCase)
    Set(lemmas:_*).toList // Remove duplication
  }
}

case class SenseMapping(internalID: String, externalID: String, matchingHypernyms: List[String]) {
  val id = s"$internalID->$externalID"
  val isMapped = matchingHypernyms.nonEmpty
}

case class Sense(id: String, word: String, hypernyms: List[String]) {
  val cleanedHypernyms = SenseMapping.cleanHypernyms(hypernyms)
}
