package de.tudarmstadt.lt.wsd.common.model

import de.tudarmstadt.lt.wsd.common.model.Implicits._
import breeze.linalg.{SparseVector => BSV}
import scalikejdbc.{WrappedResultSet, sqls, _}
import skinny.orm.{Alias, SkinnyNoIdMapper}
/**
  * Created by fide on 24.04.17.
  */

case class SenseVector(sense_id: String, model: String, inventory: String, vector: BSV[Double], sense: Sense)

object SenseVector extends SkinnyNoIdMapper[SenseVector] {
  override val tableName = "sense_vectors"
  override lazy val defaultAlias: Alias[SenseVector] = createAlias("sv")

  val sv: Alias[SenseVector] = defaultAlias
  val s: Alias[Sense] = Sense.defaultAlias

  hasOneWithFkAndJoinCondition[Sense](
    right = Sense,
    fk = "sense_id",
    on = sqls.eq(sv.inventory, s.inventory)
      .and.eq(sv.sense_id, s.sense_id),
    merge = (sv, s) => sv.copy(sense = s.get)
  ).byDefault // TODO is copy expensive?

  override def extract(rs: WrappedResultSet, n: scalikejdbc.ResultName[SenseVector]) = new SenseVector(
    sense_id = rs.string(n.sense_id),
    model = rs.string(n.model),
    inventory = rs.string(n.inventory),
    vector = rs.binaryStream(n.vector),
    sense = Sense(rs)
  )

  def findAllByModelAndCaseIgnoredWord(model: SenseVectorModel, word: String): List[SenseVector] =
    findAllBy(sqls.eq(sv.model, model.toDbModelString).and.eq(sqls"LOWER(${s.word})", word.toLowerCase()))

  def findByModelAndId(model: SenseVectorModel, sense_id: String): Option[SenseVector] =
    findBy(sqls.eq(sv.model, model.toDbModelString).and.eq(sv.sense_id, sense_id))

  def findAllByModelAndIds(model: SenseVectorModel, sense_ids: List[String]): List[SenseVector] =
    findAllBy(sqls.eq(sv.model, model.toDbModelString).and.in(sv.sense_id, sense_ids))

  def findAllByModel(model: SenseVectorModel): List[SenseVector] =
    findAllBy(sqls.eq(sv.model, model.toDbModelString))
}