package de.tudarmstadt.lt.wsd.common.model

import de.tudarmstadt.lt.wsd.common.utils.SkinnyMapperWithStringId
import scalikejdbc.{WrappedResultSet, sqls, autoConstruct}
import skinny.orm.Alias

/**
  * Created by fide on 02.12.16.
  */
case class IndexedFeature(label: String, index: Int, model: String)

object IndexedFeature extends SkinnyMapperWithStringId[IndexedFeature] {
  override val tableName = "word_vector_feature_indices"

  override lazy val defaultAlias: Alias[IndexedFeature] = createAlias("i")
  val i: Alias[IndexedFeature] = defaultAlias
  override val primaryKeyFieldName = "label"

  override def extract(rs: WrappedResultSet, n: scalikejdbc.ResultName[IndexedFeature]): IndexedFeature =
    autoConstruct(rs, n)

  def findAllByModelSorted(model: String): List[IndexedFeature] =
    findAllBy(sqls.eq(i.model, model), Seq(i.index)) // FIXME debug that same as: sortWith(_.index < _.index)
}