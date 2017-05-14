package de.tudarmstadt.lt.wsd.common.model

import breeze.linalg.{SparseVector => BSV}
import de.tudarmstadt.lt.wsd.common.{Feature, FeatureVectorizer}
import de.tudarmstadt.lt.wsd.common.model.Implicits._
import scalikejdbc._
import skinny.orm.SkinnyNoIdMapper

/**
  * Created by fide on 06.12.16.
  */
case class WordVector(word: String, model: String, vector: BSV[Double]) {
  def unvectorize: Seq[Feature] = {
    // WARNING: this will load all indexed features and might be slow and consume substantial memory
    val vectorizer = FeatureVectorizer.getVectorizer(WordVector.withName(model))
    vectorizer.doUnvectorize(vector)
  }
}

object WordVector extends Enumeration with SkinnyNoIdMapper[WordVector] {
  type ModelName = Value
  val coocdeps, coocwords, self, any = Value

  implicit def implicitToString: ModelName => String = (t: ModelName) => t.toString

  override val tableName = "word_vectors"
  override lazy val defaultAlias = createAlias("vw")
  val vw = defaultAlias

  override def extract(rs: WrappedResultSet, n: scalikejdbc.ResultName[WordVector]): WordVector = autoConstruct(rs, n)

  def findAllByModelAndWord(modelName: String, word: String) = findAllBy(sqls.eq(vw.model, modelName).and.eq(vw.word, word))
  def findByModelAndWord(modelName: String, word: String) = findBy(sqls.eq(vw.model, modelName).and.eq(vw.word, word))

}