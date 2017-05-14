package de.tudarmstadt.lt.wsd.common.model

import scalikejdbc._
import skinny.orm.{Alias, SkinnyNoIdMapper}
import de.tudarmstadt.lt.wsd.common.model.Implicits._


case class WeightedWord(word: String, weight: Double) {
  override def toString = s"$word:$weight"
}

object WeightedWord {
  def apply(tuple: (String, Double)):WeightedWord = tuple match { case (s, d) => WeightedWord(s, d) }
}

/*
  Note: this needs a setup: add babelnet_id column and fill with data:
  @See scripts/integration_test.sh
 */
case class Sense(sense_id: String,
                 inventory: String,
                 word: String,
                 hypernyms: Seq[String],
                 weighted_hypernyms: Seq[WeightedWord],
                 cluster_words: Seq[String],
                 weighted_cluster_words: Seq[WeightedWord],
                 babelnet_id: Option[String] = None
                ) {
  val num_id: Int = -1 // FIXME

  def isInventoryCoset: Boolean = Sense.isInventoryCoset(Sense withName inventory)
}

object Sense extends Enumeration with SkinnyNoIdMapper[Sense] {

  def isInventoryCoset(inventoy: InventoryName): Boolean =
    Seq(Sense.cosets1k, Sense.cosets2k).contains(inventoy)

  type InventoryName = Value
  val traditional, cosets2k, cosets1k, any = Value

  implicit def implicitToString: InventoryName => String = (t: InventoryName) => t.toString

  override val tableName = "senses"
  override lazy val defaultAlias: Alias[Sense] = createAlias("s")
  val s: Alias[Sense] = defaultAlias

  override def extract(rs: WrappedResultSet, n: scalikejdbc.ResultName[Sense]): Sense = autoConstruct(rs, n)
  def findAllByCaseIgnoredWord(word: String): List[Sense] =
    findAllBy(sqls.eq(sqls"LOWER(${s.word})", word.toLowerCase()))

  def findByInventoryAndId(inventory: InventoryName, id: String): Option[Sense] =
    findBy(sqls.eq(s.inventory, inventory.toString).and.eq(s.sense_id, id))

}
