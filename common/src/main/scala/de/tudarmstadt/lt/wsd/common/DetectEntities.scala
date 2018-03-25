package de.tudarmstadt.lt.wsd.common

import com.typesafe.scalalogging.slf4j.LazyLogging
import de.tudarmstadt.lt.wsd.common.utils.ScalaNLPUtils
import scalikejdbc._

/*
  Note: this needs a setup: fill DB table with entities
  @See scripts/build_toy_model.sh or scripts/build_toy_model.sh function import_db_entities()

*/
object DetectEntities extends LazyLogging {

  val OTHER_TAG = "O"

  def get(text: String): List[(String, String)] = {
    val mergedEntities = getEntitiesFromScalaNLP(text) ::: getEntitiesFromDB(text) ::: getNounsFromScalaNLP(text)

    val entities = removeOverlapping(mergedEntities)
    val filled = fillIndexGaps(entities, text.length).map{case (start, end, ner) => (text.substring(start, end), ner)}
    logger.debug("GET ENTITIES:\n"+
      "ALL: " + mergedEntities.mkString(" // ") + "\n" +
      "NO OVERLAPPING:\n" + entities.mkString(" // ") + "\n" +
      "FILLED:\n" + filled.mkString(" // ")
    )
    filled
  }

  def fillIndexGaps(entities: List[(Int, Int, String)], last: Int): List[(Int, Int, String)] = {
    var nextStart = 0

    val filled = entities.flatMap{ case (start, end, ner) =>
      val beforeStart = start - 1
      val result = if (nextStart < beforeStart) {
        List((nextStart, beforeStart, OTHER_TAG), (start, end, ner))
      } else {
        List((start, end, ner))
      }
      nextStart = end + 1
      result
    }

    if (nextStart < last)
      filled :::  List((nextStart, last, OTHER_TAG))
    else
      filled
  }

  def removeOverlapping(entities: List[(Int, Int, String)]): List[(Int, Int, String)] = {
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

  /***
    *
    */
  def getEntitiesFromDB(text: String)(implicit session: DBSession = null): List[(Int, Int, String)] = {
    DB readOnly { implicit session =>

      def subsetsShrinkingEnd[T](set: List[T]) = (for (i <- set.indices) yield set.drop(i)).toList
      def subsetsShrinkingBegin[T](set: List[T]) = subsetsShrinkingEnd(set.reverse).map(_.reverse)

      // Build some kind of power set from a list, however the order of elements matters, like this example:
      // List(My, small, example)
      // =>
      // List(
      //      List(My, small, example),
      //      List(My, small),
      //      List(small, example),
      //      List(My),
      //      List(example),
      //      List(small)
      // )
      def subsetsShrinkingBoth[T](set: List[T]) = subsetsShrinkingEnd(set).flatMap(subsetsShrinkingBegin)

      val indices = ScalaNLPUtils.convertToTokenIndices(text).map{ case (start, end, _) => (start, end) }


      /* TODO: DELETE
      lowerText.foldLeft(List((0,0,""))){ case (cur :: prev, char) =>
        if (char == ' ') {
          val (_, end, token) = cur
          // If token is empty we have subsequent
          val head = if (token.isEmpty) List(cur) else List((end + 2, end + 2,""), cur)
          head ::: prev
        } else {
          val (start, end, token) = cur
          List((0,0,""))  TODO finish if needed
        }
      }
      */
      val lowerText = text.toLowerCase()
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


  def getEntitiesFromScalaNLP(text: String): List[(Int, Int, String)] = {
    val joinedTokens = ScalaNLPUtils.convertToIndexedNERs(text)
    val TAGS = ScalaNLPUtils.TAGS
    val ignoredTags = List(
      TAGS.Cardinal,
      TAGS.Date,
      TAGS.Money,
      TAGS.Ordinal,
      TAGS.Percent,
      TAGS.OutsideSentence,
      TAGS.Quantity,
      TAGS.Time
    ).map(_.toString)

    joinedTokens.filterNot(x => ignoredTags.contains(x._3))
  }

  def getNounsFromScalaNLP(text: String): List[(Int, Int, String)] = {
    val posTags = ScalaNLPUtils.convertToPOS(text)
    posTags.filter(_._3.startsWith("NN")) // match NN, NNS, NNP
  }
}