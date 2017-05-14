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
object NLPUtils {
  val OTHER_TAG = "O"
  val lemmatizerProps = new Properties()
  lemmatizerProps.setProperty("annotators", "tokenize, ssplit, pos, lemma")
  lazy val lemmatizer = new StanfordCoreNLP(lemmatizerProps)


  val nerParserProps = new Properties()
  nerParserProps.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner")
  lazy val nerParser = new StanfordCoreNLP(nerParserProps)

  val depParserProps = new Properties()
  depParserProps.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, relation")
  lazy val depParser = new StanfordCoreNLP(depParserProps)

  val convertToLemmas = convertToLemmasWithStanfordCoreNLP331 _
  val convertToLemmaPosTuples = convertToLemmasAndPosWithStanfordCoreNLP331 _
  val convertToJoinedTokensWithNERsTuples = convertToJoinedTokensAndNERsWithStanfordCoreNLP331 _

  def convertToIndexedNERsWithStanfordCoreNLP331(text: String): List[(Int, Int, String)] = {
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

  def mergeTokensToString(text: String, tokens: CoreLabel*): String = {
    text.substring(tokens.head.beginPosition(), tokens.last.endPosition())
  }


  def convertToJoinedTokensAndNERsWithStanfordCoreNLP331(text: String): List[(String, String)] = {
    convertToIndexedNERsWithStanfordCoreNLP331(text).map{
      case (start, end, ner) => (text.substring(start, end), ner)
    }
  }

  def convertToLemmasWithStanfordCoreNLP331(text: String): List[String] = {
    //http://nlp.stanford.edu/pubs/StanfordCoreNlp2014.pdf
    val annotation = new Annotation(text)
    lemmatizer.annotate(annotation)
    val sentence = annotation.get(classOf[SentencesAnnotation]).toList.headOption
    val tokens = sentence.map(_.get(classOf[TokensAnnotation]))
    val lemmas = tokens.map(_.toList.map(_.get(classOf[LemmaAnnotation])))

    lemmas.getOrElse(List.empty[String])
  }

  def convertToLemmasAndPosWithStanfordCoreNLP331(text: String): List[Tuple2[String, String]] = {
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
  def convertToDepRelationsWithStanfordCoreNLP331(text: String): List[util.List[Pair[Triple[String, String, String], String]]] = {
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
