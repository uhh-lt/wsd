package de.tudarmstadt.lt.wsd.common.model

import java.io.InputStream
import java.sql.ResultSet

import breeze.linalg.{SparseVector => BSV}
import com.twitter.chill.KryoInjection
import de.tudarmstadt.lt.wsd.common.utils.JoBimTextTSVUtils._
import org.apache.commons.io.IOUtils
import scalikejdbc.TypeBinder

/**
  * Created by fide on 09.04.17.
  */
object Implicits {
  implicit val inputStreamToBreezeSparseVector: InputStream => BSV[Double] =
    stream => {
      val vecBinary = IOUtils.toByteArray(stream)
      KryoInjection.invert(vecBinary).get.asInstanceOf[BSV[Double]]
    }

  implicit val breezeSparseVectorTypeBinder: TypeBinder[BSV[Double]] = new TypeBinder[BSV[Double]] {
    def apply(rs: ResultSet, col_name: String): BSV[Double] = rs.getBinaryStream(col_name)
    def apply(rs: ResultSet, col_idx: Int): BSV[Double] = rs.getBinaryStream(col_idx)
  }

  implicit val weightedWordSeqToString: (Seq[WeightedWord]) => String =
    (l: Seq[WeightedWord]) => l.mkString(",")

  implicit val weightedWordSeqTypeBinder: TypeBinder[Seq[WeightedWord]] =
    new TypeBinder[Seq[WeightedWord]] {
      def apply(rs: ResultSet, col_name: String): Seq[WeightedWord] =
        extrWordsWithWeights(rs.getString(col_name)).map { case (a, w) => WeightedWord(a, w) }
      def apply(rs: ResultSet, col_idx: Int): Seq[WeightedWord] =
        extrWordsWithWeights(rs.getString(col_idx)).map { case (a, w) => WeightedWord(a, w) }
    }

  implicit val weightedStringSeqTypeBinder: TypeBinder[Seq[String]] =
    new TypeBinder[Seq[String]] {
      def apply(rs: ResultSet, col_name: String): Seq[String] = rs.getString(col_name).split(",")
      def apply(rs: ResultSet, col_idx: Int): Seq[String] = rs.getString(col_idx).split(",")
    }

  implicit val positionTypeBinder: TypeBinder[Position] =
    new TypeBinder[Position] {
      def apply(rs: ResultSet, col_name: String): Position = Position(rs.getString(col_name))
      def apply(rs: ResultSet, col_idx: Int): Position = Position(rs.getString(col_idx))
    }


  // TODO really needed?
  implicit val positionToString: (Position) => String = (pos: Position) => pos.toString
}
