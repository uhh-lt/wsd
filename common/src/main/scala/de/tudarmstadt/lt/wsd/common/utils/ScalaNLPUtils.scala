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

  def convertToIndexedNERs(text: String): List[(Int, Int, String)] = {

    val tokens = sentenceSplitter(text).flatMap(tokenizer)
    val entities = ner.bestSequence(tokens)

    convertSegmentation(entities)
  }

  def convertToPOS(text: String): List[(Int, Int, String)] = {

    val tokens = sentenceSplitter(text).flatMap(tokenizer)
    val entities = tagger.bestSequence(tokens)

    convertSegmentation(entities.asSegmentation)
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
