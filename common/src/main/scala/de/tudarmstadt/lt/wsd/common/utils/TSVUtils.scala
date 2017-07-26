package de.tudarmstadt.lt.wsd.common.utils

import java.io.{File, PrintWriter}
import com.github.tototoshi.csv.{TSVFormat, CSVReader, CSVWriter}
/**
  * Created by fide on 12.01.17.
  */
object TSVUtils {

  implicit object JoBimTextFormat extends TSVFormat {
    override val quoteChar: Char = '元'
    override val escapeChar: Char = '元'
  }

  def readWithHeaders(path: String, removeIncomplete: Boolean = false): (List[String], List[Map[String, String]]) = {
    val reader = CSVReader.open(path)
    val (headers, rows) = reader.allWithOrderedHeaders()
    if (removeIncomplete) {
      val headersSet = headers.toSet
      (headers, rows.filter(_.keySet == headersSet))
    } else {
      (headers, rows)
    }
  }

  def readHeaders(path: String): List[String] = {
    val lines = scala.io.Source.fromFile(new File(path), "UTF8").getLines()
    val headers = lines.next().split("\t").toList
    headers
  }

  def writeWithHeaders(path: String, rows: Seq[Map[String, String]], headers: Seq[String]): Unit = {
    val writer = CSVWriter.open(path)
    val withOrderedHeaders = rows.map(m => headers.map(h => m(h))).toSeq
    writer.writeRow(headers)
    writer.writeAll(withOrderedHeaders)
  }

  def write(path: String, rows: Seq[Seq[String]]): Unit = {
    val writer = CSVWriter.open(path)
    writer.writeAll(rows)
  }

}
