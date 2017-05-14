package de.tudarmstadt.lt.wsd.common

import de.tudarmstadt.lt.wsd.common.utils.NLPUtils
import de.tudarmstadt.lt.wsd.common.utils.NLPUtils.OTHER_TAG
import scalikejdbc._

/*
  Note: this needs a setup: fill DB table with entities
  @See scripts/integration_test.sh
*/
object DetectEntities {

  def get(text: String) = {
    val mergedEntities = getEntitiesFromStanford(text) ::: getEntitiesFromDB(text) ::: getNounsFromStanford(text)
    val entities = removeOverlapping(mergedEntities)
    fillIndexGaps(entities).map{case (start, end, ner) => (text.substring(start, end), ner)}
  }

  def fillIndexGaps(entities: List[(Int, Int, String)]) = {
    var nextStart = 0

    entities.flatMap{ case (start, end, ner) =>
      val beforeStart = start - 1
      val result = if (nextStart < beforeStart) {
        List((nextStart, beforeStart, OTHER_TAG), (start, end, ner))
      } else {
        List((start, end, ner))
      }
      nextStart = end + 1
      result
    }
  }

  def removeOverlapping(entities: List[(Int, Int, String)]) = {
    entities.sortBy(_._1).foldLeft(List[(Int, Int, String)]()) { case (list, (s, e, n)) =>
      list match {
        case (start, end, _) :: tail if s > end => (s, e, n) :: list
        case (start, end, _) :: tail if s <= end && (e - s) > (end - start) => (s, e, n) :: tail
        case (start, end, _) :: tail if s <= end && (e - s) <= (end - start) => list
        case Nil => (s, e, n) :: Nil
      }
    }.reverse
  }

  def mergeIndicesToString(text: String, tokens: (Int, Int)*): String = {
    text.substring(tokens.head._1, tokens.last._2)
  }

  def getEntitiesFromDB(text: String)(implicit session: DBSession = null): List[(Int, Int, String)] = {
    DB readOnly { implicit session =>


      def subsetsShrinkingEnd[T](set: List[T]) = (for (i <- set.indices) yield set.drop(i)).toList
      def subsetsShrinkingBegin[T](set: List[T]) = subsetsShrinkingEnd(set.reverse).map(_.reverse)
      def subsetsShrinkingBoth[T](set: List[T]) = subsetsShrinkingEnd(set).flatMap(subsetsShrinkingBegin)

      val indices = NLPUtils.convertToTokenIndices(text)

      val lowerText = text.toLowerCase()
      val splitted = lowerText.split(" ").toList
      //val subsets = subsetsShrinkingBoth(splitted).map(_.mkString(" "))
      val subsets = subsetsShrinkingBoth(indices).map(mergeIndicesToString(lowerText, _:_*))

      val entities =
        sql"SELECT * FROM entities WHERE text IN (${subsets})"
          .map(rs => rs.string("text")).list.apply()
      entities.map { e =>
        val index = lowerText.indexOfSlice(e)
        (index, index + e.length, "MULTI-WORD")
      }
    }
  }

  def getEntitiesFromStanford(text: String) = {
    val joinedTokens = NLPUtils.convertToIndexedNERsWithStanfordCoreNLP331(text)
    joinedTokens.filter{case (start, end, entityName) => entityName != OTHER_TAG}
  }

  def getNounsFromStanford(text: String) = {
    val posTags = NLPUtils.convertToPOS(text)
    posTags.filter(_._3.startsWith("NN")) // match NN, NNS, NNP
  }

}