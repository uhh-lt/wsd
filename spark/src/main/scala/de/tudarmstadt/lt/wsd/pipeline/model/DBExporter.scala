package de.tudarmstadt.lt.wsd.pipeline.model
import java.io.File

import de.tudarmstadt.lt.wsd.common.model.{IndexedFeature, Sense, SenseVectorModel, WeightedWord}
import de.tudarmstadt.lt.wsd.pipeline.utils.RuntimeContext
import de.tudarmstadt.lt.wsd.pipeline.utils.SparkUtils._
import org.apache.spark.sql.{Row, SaveMode}
import org.apache.spark.sql.functions._

/**
  * Created by fide on 10.12.16.
  */
object DBExporter extends RuntimeContext {

  import spark.implicits._
  import de.tudarmstadt.lt.wsd.common.utils.DBUtils._

  private val mkStringUDF = udf { arr: Seq[Any] => arr.map(_.toString).mkString(",")}
  private def mkStringWordWeightSeqUDF = udf { seq: Seq[Row] =>
    seq.map(
      row => WeightedWord(row.getAs[String]("word"), row.getAs[Double]("weight"))
    ).mkString(",")
  }

  def exportSenseInventory(path: String, name: String): Unit = {

    spark.read.parquet(path)
      //.as[Sense]
      .withColumn("hypernyms", mkStringUDF('hypernyms))
      .withColumn("weighted_hypernyms", mkStringWordWeightSeqUDF('weighted_hypernyms))
      .withColumn("cluster_words", mkStringUDF('cluster_words))
      .withColumn("weighted_cluster_words", mkStringWordWeightSeqUDF('weighted_cluster_words))
      .write.mode(SaveMode.Append)
      .jdbc(jdbcUrl, "senses", emptyProps)
  }

  def exportSenseVectorModel(path: String, name: String): Unit = {
    val config = SenseVectorModel.parseFromString(name)
    spark.read.parquet(path)
      .withColumn("model", lit(config.toString))
      .withColumn("inventory", lit(config.sense_inventory.toString))
      //.as[CommonSenseVector]
      .withColumn("vector", serializeVecUDF('vector))  // Maybe possible with encoder?
      .select("sense_id", "inventory", "vector", "model")
      .write.mode(SaveMode.Append)
      .jdbc(jdbcUrl, "sense_vectors", combatBatchSizeProps)

  }

  def exportWordVectorModel(path: String, name: String): Unit = {

    spark.read.parquet(s"$path/index")
      .withColumn("model", lit(name))
      .as[IndexedFeature]
      .write.mode(SaveMode.Append)
      .jdbc(jdbcUrl, "word_vector_feature_indices", emptyProps)

    spark.read.parquet(s"$path/vectors")
      .withColumn("model", lit(name))
      //.as[CommonWordVector]
      .withColumn("vector", serializeVecUDF('vector))
      .select("word", "vector", "model")
      .write.mode(SaveMode.Append)
      .jdbc(jdbcUrl, "word_vectors", combatBatchSizeProps)
  }

  def autoExportAllModels(parquetLocation: String): Unit = {
    val pathAndNamesInDir = (dir: String) =>
      new File(dir).listFiles().filter(_.isDirectory).map(d => (d.getCanonicalPath, d.getName))

    val senseInventoryLocation = s"$parquetLocation/sense_inventories"
    val wordVectorLocation = s"$parquetLocation/word_vectors"
    val senseVectorLocation = s"$parquetLocation/sense_vectors"

    pathAndNamesInDir(senseInventoryLocation).foreach((exportSenseInventory _).tupled)
    pathAndNamesInDir(wordVectorLocation).foreach((exportWordVectorModel _).tupled)
    pathAndNamesInDir(senseVectorLocation).foreach((exportSenseVectorModel _).tupled)
  }
}
