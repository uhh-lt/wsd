package de.tudarmstadt.lt.wsd.common.utils

import java.util
import java.util.Properties

import epic.ontonotes.NerType
import epic.preprocess.MLSentenceSegmenter
import epic.sequences.Segmentation
import epic.slab.{Sentence, Slab, Token}
import epic.trees.Span

import scala.collection.JavaConversions._

/**
  * Created by fide on 02.12.16.
  */
object ScalaNLPUtils {
  val TAGS = NerType

  private lazy val sentenceSplitter = MLSentenceSegmenter.bundled().get
  private lazy val tokenizer = new epic.preprocess.TreebankTokenizer()
  private lazy val tagger = epic.models.PosTagSelector.loadTagger("en").get
  private lazy val ner = epic.models.NerSelector.loadNer("en").get

  // lazy val depParser = _

  def convertToLemmas(text: String): List[String] = sentenceSplitter(text).flatMap(tokenizer).toList

  private def tokenWithRealOffsets(text: String): List[(Int, Int, String)] = {
    val tokens = convertToLemmas(text)
    val (_, result) = tokens.foldLeft(text, List[(Int, Int, String)]()) {
      case ((haystack, current), token) =>
        val offset = current.headOption.map(_._2).getOrElse(0)
        val start = haystack indexOf token
        val end = start + token.length

        (haystack.substring(end), current :+ (start + offset, end + offset, token))
    }
    result
  }

  private def epicToRealOffsets(text: String, segments:  List[(Int, Int, String)]):  List[(Int, Int, String)] = {
    val real = tokenWithRealOffsets(text)
    segments.map{
      case (start, end, token) =>
        val (realStart, _, _) = real(start)
        val (_, realEnd, _) = real(end-1)
        (realStart, realEnd, token)
    }
  }

  def convertToIndexedNERs(text: String): List[(Int, Int, String)] = {

    val tokens = sentenceSplitter(text).flatMap(tokenizer)
    val entities = ner.bestSequence(tokens)

    val segments = convertSegmentation(entities)
    epicToRealOffsets(text, segments)
  }

  def convertToPOS(text: String): List[(Int, Int, String)] = {

    val tokens = sentenceSplitter(text).flatMap(tokenizer)

    // FIXME https://github.com/dlwh/epic/issues/57
    val entities = tagger.bestSequence(tokens)

    val segments = convertSegmentation(entities.asSegmentation)

    epicToRealOffsets(text, segments)
  }

  def convertToTokenIndices(text: String): List[(Int, Int, String)] = {
    // Copied form epic.preprocess.Tokenizer.apply
    val slab = tokenizer(Slab(text).append(Span(0, text.length), Sentence()))
    val tokens = slab.iterator[Token]

    tokens.map{ case (span, token) => (span.begin, span.end, token.token)}.toList
  }

  private def convertSegmentation(segmentation: Segmentation[Any, String]) =
    segmentation.segmentsWithOutside.flatMap {
      case (None, span) => None
      case (Some(l), span) => Some(span.begin, span.end, l.toString)
    }.toList




}
