package de.tudarmstadt.lt.wsd.common.model

import de.tudarmstadt.lt.wsd.common.model.Implicits._
import scalikejdbc.{WrappedResultSet, autoConstruct}
import skinny.orm.{Alias, SkinnyNoIdMapper}

/**
  * Created by fide on 24.04.17.
  */

case class SampleSentence(sense_id: String, inventory: String, sentence: String,
                          sentence_id: Int, sense_position: Position)

object SampleSentence extends SkinnyNoIdMapper[SampleSentence] {
  override val tableName = "sample_sentences"
  override lazy val defaultAlias: Alias[SampleSentence] = createAlias("ss")

  val ss: Alias[SampleSentence] = defaultAlias

  override def extract(rs: WrappedResultSet, n: scalikejdbc.ResultName[SampleSentence]): SampleSentence = autoConstruct(rs, n)

}

case class Position(start: Int, end: Int) {
  override def toString: String = s"$start,$end"
}

object Position {

  // Construct from comma separated strings like: 9,21
  def apply(string: String): Position = string.split(',') match {
    case Array(start, end) => Position(start.toInt, end.toInt)
  }
}
