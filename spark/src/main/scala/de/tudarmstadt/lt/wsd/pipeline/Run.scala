package de.tudarmstadt.lt.wsd.pipeline
import de.tudarmstadt.lt.wsd.common.model.{Sense, SenseVectorModel}
import de.tudarmstadt.lt.wsd.common.{Run => CommonRun}
import java.io.File

import com.typesafe.config.ConfigFactory
import de.tudarmstadt.lt.wsd.pipeline.model._
import scalikejdbc.config.DBs
/**
  * Created by fide on 25.11.16.
  */

// Performance considerations:
// - is performance acceptable if hypernyms is kept => seems so
// - is kryochill fast and small => 30min for convertion, size approx the same => ok
// - how fast is db insert => 5min very fast
object Run {

  private val config = ConfigFactory.load()

  private val defaultTrainingSetLocation = config.getString("wsd.spark.training_set_location")
  private val featureFileName = config.getString("wsd.spark.term_features_file_name")
  private val clusterFileName = config.getString("wsd.spark.word_sense_file_name")
  private val coSetFileName = config.getString("wsd.spark.coset_file_name")
  private val coSet2kFileName = config.getString("wsd.spark.coset2k_file_name")

  val defaultFeatureFile = s"$defaultTrainingSetLocation/$featureFileName"
  val defaultTraditionalClusterFile = s"$defaultTrainingSetLocation/$clusterFileName"
  val defaultCoSet1kClusterFile = s"$defaultTrainingSetLocation/$coSetFileName"
  val defaultCoSet2kClusterFile = s"$defaultTrainingSetLocation/$coSet2kFileName"

  private val defaultModelLocation = config.getString("wsd.model_location")


  object Command extends Enumeration {
    type Name = Value
    val create, exportdb, nocmd = Value
  }

  import Command._

  case class Params(mode: Command.Name = Command.nocmd,
                    modelName: Option[SenseVectorModel] = None,
                    parquetModelLocation: String = defaultModelLocation,
                    featureFile: File = new File(defaultFeatureFile),
                    clusterFile: Option[File] =  None
                   )

  val clusterFiles = Map(
    Sense.traditional -> defaultTraditionalClusterFile,
    Sense.cosets1k -> defaultCoSet1kClusterFile,
    Sense.cosets2k -> defaultCoSet2kClusterFile
  )

  implicit val commandsRead: scopt.Read[Command.Value] =
    scopt.Read.reads(Command withName)


  implicit val modelSenseVectorConfigRead: scopt.Read[SenseVectorModel] =
    scopt.Read.reads(SenseVectorModel.parseFromString)

  val parser = new scopt.OptionParser[Params]("wsd-spark") {
    head("wsp-spark", "0.3.x")

    opt[String]('p', "parquetLocation").action((x, c) =>
      c.copy(parquetModelLocation = x))
      .text(s"Location from where to write and read parquet, default: $defaultModelLocation")

    cmd(create.toString).action( (_, c) => c.copy(mode = create) ).
      text("Creates a prediction model and saves it with parquet format").
      children(
        opt[SenseVectorModel]('n', "modelName").required().action((x, c) =>
          c.copy(modelName = Some(x))).text(s"Name of the model to create (TODO)"),
        opt[String]('f', "featureFile").action((x, c) =>
          c.copy(featureFile = new File(x)))
          .text(s"Path to the feature file for training, default: $defaultFeatureFile"),
        opt[String]('c', "clusterFile").action((x, c) =>
          c.copy(clusterFile = Some(new File(x))))
          .text(s"Path to the cluster file for training, default depends on modelName. See note below.")
      )

    note(s"\nNote: the default cluster file depends on the model:\n${
      clusterFiles.groupBy(_._2).map{case (f, m) => s"${m.keys.mkString(" and ")} -> $f"}.mkString("\n")
    }\n")

    cmd(exportdb.toString).action( (_, c) => c.copy(mode = exportdb) ).
      text("Export parquet model to DB")

    checkConfig {
      case Params(Command.nocmd, _, _, _, _) => failure("No command given.")
      case _ => success
    }
  }


  def main(args: Array[String]): Unit = {
    parser.parse(args, Params()) match {
      case Some(params) =>
        params.mode match {
          case `create` =>
            createModel(params)
          case `exportdb` =>

            DBs.setupAll()

            DBExporter.autoExportAllModels(
              params.parquetModelLocation
            )
            CommonRun.mainIndexDB()
        }

      case None =>
      // arguments are bad, error message will be displayed by scopt
    }
  }

  def createModel(params: Params): Unit = {
    val featurePath = params.featureFile.getCanonicalPath
    val modelLocation = params.parquetModelLocation
    val config = params.modelName.get
    val clusterPath = params.clusterFile
      .getOrElse(new File(clusterFiles(config.sense_inventory))).getCanonicalPath

    val builder = new SenseVectorModelBuilder()
      .setModelLocation(modelLocation)
      .setSenseClustersCVSPath(clusterPath)
      .setContextFeaturesCVSPath(featurePath)
      .setInventoryName(config.sense_inventory)
      .setWordFeature(config.word_vector_model)

    builder.load().build().save()
  }
}
