package de.tudarmstadt.lt.wsd.common.utils

/**
  * Created by fide on 01.12.16.
  */
object StringUtils {

  def prettySPrintList(list: List[String], maxElements: Int = 5): String = {
    s"""${list.slice(0,maxElements).mkString("', '")}'
       |${if (list.length > maxElements) s", ... (${list.length - maxElements} more)" else ""}
     """.stripMargin

  }

  def extractFirstWords(someComplexToken: String): String = {
    val pattern = """^[a-z][-_a-z ]+""".r
    val str = someComplexToken.toLowerCase
    pattern.findAllIn(str).toSeq.headOption.getOrElse("")
  }

  def extractFirstToken(someComplexToken: String): String = {
    val pattern = """^[a-z][-_a-z]+""".r
    val str = someComplexToken.toLowerCase
    pattern.findAllIn(str).toSeq.headOption.getOrElse("")
  }

  val splitOnCommaAndRemoveEachAfterColon = {
    str: String => str.split(",").map(s => s.take(s.indexOf(':')))
  }

  val splitOnCommaAndExtractEachFirstToken = {
    str: String => str.split(",").map(s => extractFirstToken(s.trim)).filter(_.nonEmpty)
  }

  def formatTable(lines: List[Tuple2[String, Any]]): String = {
    val keyColWidth = 25
    val valColWidth = 50
    val lineFormat = s"%-${keyColWidth}s: %s"
    val doFormatLine = (k: String, v: Any) => {
      val valStr = v.toString
      if (valStr.length > valColWidth) {
        val splittedLines = splitLinesBeforeMaxLength(valStr, valColWidth)
        val whiteSpaces = " " * keyColWidth
        val secondLineFormat = s"$whiteSpaces  %s"
        val linesAfterFirst = splittedLines.drop(1).map(l => secondLineFormat.format(l))
        val firstLine = lineFormat.format(k, splittedLines.head)
        s"$firstLine\n${linesAfterFirst.mkString("\n")}"
      } else {
        lineFormat.format(k, valStr)
      }

    }
    lines.map{case (k,v) => doFormatLine(k, v)}.mkString("\n")
  }

  def splitTooLongOnSpace(text: String, maxLength: Int): Tuple2[String, Option[String]] = {
    if (text.length <= maxLength) {
      (text, None)
    } else {
      var splitIndex = text.lastIndexOf(' ', maxLength)
      if (splitIndex < 0) {
        splitIndex = text.indexOf(' ')
      }
      if (splitIndex < 0) {
        (text, None)
      } else {
        val firstPart = text.substring(0, splitIndex-1)
        val secondPart = text.substring(splitIndex+1)
        (firstPart, Some(secondPart))
      }
    }
  }

  def splitLinesBeforeMaxLength(line: String, maxLength: Int): List[String] = {
    var remainingPart = Option(line)
    var lines: List[String] = List()

    while (remainingPart.nonEmpty) {
      val result = splitTooLongOnSpace(remainingPart.get, maxLength)
      val splittedLine = result._1
      remainingPart = result._2
      lines = lines :+ splittedLine
    }

    lines
  }

}
