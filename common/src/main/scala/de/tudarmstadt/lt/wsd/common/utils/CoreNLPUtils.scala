package de.tudarmstadt.lt.wsd.common.utils
import java.util
import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations._
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.util.{Pair, Triple}

import scala.collection.JavaConversions._

/**
  * Created by fide on 02.12.16.
  */
@deprecated
object CoreNLPUtils {
  val OTHER_TAG = "O"
  val ORGANIZATION_TAG = "ORGANIZATION"
  val DATE_TAG = "DATE"

  private val lemmatizerProps = new Properties()
  lemmatizerProps.setProperty("annotators", "tokenize, ssplit, pos, lemma")
  private lazy val lemmatizer = new StanfordCoreNLP(lemmatizerProps)


  private val nerParserProps = new Properties()
  nerParserProps.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner")
  private lazy val nerParser = new StanfordCoreNLP(nerParserProps)

  private val depParserProps = new Properties()
  depParserProps.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, relation")
  private lazy val depParser = new StanfordCoreNLP(depParserProps)

  val convertToLemmas: (String) => List[String] = convertToLemmasWithStanfordCoreNLP331 _
  val convertToIndexedNERs: (String) => List[(Int, Int, String)] = convertToIndexedNERsWithStanfordCoreNLP331 _

  def convertToTokenIndices(text: String): List[(Int, Int)] = {
    val annotation = new Annotation(text)
    nerParser.annotate(annotation)
    val sentence = annotation.get(classOf[SentencesAnnotation]).toList.head
    val tokens = sentence.get(classOf[TokensAnnotation]).toList

    tokens.map(t => (t.beginPosition(), t.endPosition()))
  }

  def convertToPOS(text: String): List[(Int, Int, String)] = {
    val annotation = new Annotation(text)
    nerParser.annotate(annotation)
    val sentence = annotation.get(classOf[SentencesAnnotation]).toList.head
    val tokens = sentence.get(classOf[TokensAnnotation]).toList
    val pos = tokens.map(_.get(classOf[PartOfSpeechAnnotation]))

    tokens zip pos map {case (t, p) => (t.beginPosition(), t.endPosition(), p)}
  }



  private def convertToLemmasWithStanfordCoreNLP331(text: String): List[String] = {
    //http://nlp.stanford.edu/pubs/StanfordCoreNlp2014.pdf
    val annotation = new Annotation(text)
    lemmatizer.annotate(annotation)
    val sentence = annotation.get(classOf[SentencesAnnotation]).toList.headOption
    val tokens = sentence.map(_.get(classOf[TokensAnnotation]))
    val lemmas = tokens.map(_.toList.map(_.get(classOf[LemmaAnnotation])))

    lemmas.getOrElse(List.empty[String])
  }

  private def convertToIndexedNERsWithStanfordCoreNLP331(text: String): List[(Int, Int, String)] = {
    val annotation = new Annotation(text)
    nerParser.annotate(annotation)
    val sentence = annotation.get(classOf[SentencesAnnotation]).toList.head
    val tokens = sentence.get(classOf[TokensAnnotation]).toList

    tokens.foldLeft(List[(Int, Int, String)]()) { case (list, t) =>
      list match {
        case (start, end, ner) :: tail if ner == t.ner => (start, t.endPosition(), t.ner) :: tail
        case _ => (t.beginPosition(), t.endPosition(), t.ner) :: list
      }
    }.reverse
  }


  @deprecated
  private def mergeTokensToString(text: String, tokens: CoreLabel*): String = {
    text.substring(tokens.head.beginPosition(), tokens.last.endPosition())
  }

  @deprecated
  private def convertToJoinedTokensAndNERsWithStanfordCoreNLP331(text: String): List[(String, String)] = {
    convertToIndexedNERsWithStanfordCoreNLP331(text).map{
      case (start, end, ner) => (text.substring(start, end), ner)
    }
  }


  @deprecated
  private def convertToLemmasAndPosWithStanfordCoreNLP331(text: String): List[Tuple2[String, String]] = {
    //http://nlp.stanford.edu/pubs/StanfordCoreNlp2014.pdf
    val annotation = new Annotation(text)
    lemmatizer.annotate(annotation)
    val sentence = annotation.get(classOf[SentencesAnnotation]).toList.headOption
    val tokens = sentence.map(_.get(classOf[TokensAnnotation]))
    val lemmas = tokens.map(_.toList.map(_.get(classOf[LemmaAnnotation])))
    val pos = tokens.map(_.toList.map(_.get(classOf[PartOfSpeechAnnotation])))

    lemmas.getOrElse(List[String]()) zip pos.getOrElse(List[String]())
  }

  // FIXME: returns list of null values
  @deprecated
  private def convertToDepRelationsWithStanfordCoreNLP331(text: String): List[util.List[Pair[Triple[String, String, String], String]]] = {
    //http://nlp.stanford.edu/pubs/StanfordCoreNlp2014.pdf
    val annotation = new Annotation(text)
    depParser.annotate(annotation)
    val sentence = annotation.get(classOf[SentencesAnnotation]).head
    val tokens = sentence.get(classOf[TokensAnnotation])
    val lemmas = tokens.map(_.get(classOf[LemmaAnnotation]))
    val deps = tokens.map(_.get(classOf[DependentsAnnotation]))
    deps.toList
  }

}
