package de.tudarmstadt.lt.wsd.pipeline.feature

import org.apache.spark.annotation.Since
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.{BooleanParam, Param, ParamMap, StringArrayParam}
import org.apache.spark.ml.param.shared.{HasInputCol, HasOutputCol}
import org.apache.spark.ml.util._
import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql.types.{ArrayType, DataType, StringType, StructType}



/**
  * A feature transformer that removes rows containing a stop word.
  *
  * @note Mostly copied and slightly modified from sparks StopWordsRemover
  *
  * @see org.apache.spark.ml.feature.StopWordsRemover
  */
class StopWordsFilter (override val uid: String)
  extends Transformer with DefaultParamsWritable {

  def this() = this(Identifiable.randomUID("stopWordsFilter"))
  val filterCol: Param[String] = new Param[String](this, "filterCol", "filter column name")
  final def getFilterCol: String = $(filterCol)
  def setFilterCol(value: String): this.type = set(filterCol, value)

  /**
    * The words to be filtered out.
    * Default: English stop words
    * @see `StopWordsFilter.loadDefaultStopWords()`
    */
  val stopWords: StringArrayParam =
  new StringArrayParam(this, "stopWords", "the words to be filtered out")
  def setStopWords(value: Array[String]): this.type = set(stopWords, value)
  def getStopWords: Array[String] = $(stopWords)

  /**
    * Whether to do a case sensitive comparison over the stop words.
    * Default: false
    */
  val caseSensitive: BooleanParam = new BooleanParam(this, "caseSensitive",
    "whether to do a case-sensitive comparison over the stop words")
  def setCaseSensitive(value: Boolean): this.type = set(caseSensitive, value)
  def getCaseSensitive: Boolean = $(caseSensitive)

  setDefault(stopWords -> StopWordsFilter.loadDefaultStopWords("english"), caseSensitive -> false)

  override def transform(dataset: Dataset[_]): DataFrame = {
    val _ = transformSchema(dataset.schema) // No transformation

    val f = if ($(caseSensitive)) {
      val stopWordsSet = $(stopWords).toSet
      udf { term: String => !stopWordsSet.contains(term) }
    } else {
      // TODO: support user locale (SPARK-15064)
      val toLower = (s: String) => if (s != null) s.toLowerCase else s
      val lowerStopWords = $(stopWords).map(toLower(_)).toSet
      udf { term: String => !lowerStopWords.contains(toLower(term)) }
    }
    dataset.filter(f(col($(filterCol)))).toDF()
  }

  override def transformSchema(schema: StructType): StructType = {
    val filterType = schema($(filterCol)).dataType
    require(filterType == StringType,
      s"Input type must be ArrayType(StringType) but got $filterType.")
    schema
  }

  override def copy(extra: ParamMap): StopWordsFilter = defaultCopy(extra)
}

object StopWordsFilter extends DefaultParamsReadable[StopWordsFilter] {

  private[feature]
  val supportedLanguages = Set("danish", "dutch", "english", "finnish", "french", "german",
    "hungarian", "italian", "norwegian", "portuguese", "russian", "spanish", "swedish", "turkish")

  override def load(path: String): StopWordsFilter = super.load(path)

  /**
    * Loads the default stop words for the given language.
    * Supported languages: danish, dutch, english, finnish, french, german, hungarian,
    * italian, norwegian, portuguese, russian, spanish, swedish, turkish
    * @see <a href="http://anoncvs.postgresql.org/cvsweb.cgi/pgsql/src/backend/snowball/stopwords/">
    * here</a>
    */
  def loadDefaultStopWords(language: String): Array[String] = {
    require(supportedLanguages.contains(language),
      s"$language is not in the supported language list: ${supportedLanguages.mkString(", ")}.")
    val is = getClass.getResourceAsStream(s"/org/apache/spark/ml/feature/stopwords/$language.txt")
    scala.io.Source.fromInputStream(is)(scala.io.Codec.UTF8).getLines().toArray
  }
}
