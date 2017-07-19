package de.tudarmstadt.lt.wsd.common

import java.io.File
import javax.inject.Inject

import akka.actor.ActorSystem
import skinny.Skinny
import akka.stream.ActorMaterializer
import com.typesafe.config.{ConfigFactory, ConfigRenderOptions}
import com.typesafe.scalalogging.LazyLogging
import de.tudarmstadt.lt.wsd.common.eval.{EvaluationStatistics, Evaluator, SenseInventoryMapping}
import de.tudarmstadt.lt.wsd.common.model.images.BingImageDownloader
import de.tudarmstadt.lt.wsd.common.model.{Sense, SenseVectorModel}
import de.tudarmstadt.lt.wsd.common.prediction._
import de.tudarmstadt.lt.wsd.common.utils.{FileUtils, TSVUtils, Utils}
import org.apache.commons.io.FilenameUtils
import play.api.libs.ws.ahc.AhcWSClient
import scalikejdbc._
import scalikejdbc.config._

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
  * Created by fide on 07.12.16.
  */


object Run extends LazyLogging {

  DBs.setupAll()

  object Command extends Enumeration {
    type Name = Value
    val predict, indexdb, evalbabel, evalstats, inventorystats, filterdataset, mappingstats,
    exportinventory, exporttwsiinventory, extractinventory, downloadimages, nocmd = Value
  }

  import Command._

  case class Params(mode: Command.Name = nocmd,
                    modelConfig: Option[WSDModel] = None,
                    senseVectorModelConfig: Option[SenseVectorModel] = None,
                    inputFile: String = "",
                    relatedType: RelatedType.Value = RelatedType.hypernyms,
                    useOnlyDirectDepWords: Boolean = false,
                    useNumIDs: Boolean = false,
                    filterSensesOnlyMapped: Boolean = false,
                    skipOnWordFileExists: Boolean = false,
                    rejectOption: Boolean = false,
                    senseInventoryFile: Option[String] = None,
                    secondSenseInventoryFile: Option[String] = None,
                    outputFolder: String = defaultResultLocation,
                    useGlobalInventory: Boolean = false,
                    inventoryName: Sense.InventoryName = Sense.traditional,
                    frequencyFile: String = "",
                    downloadLimit: Option[Int] = None
                   )

  val config = ConfigFactory.load()

  val defaultResultLocation = config.getString("wsd.result_location")
  val defaultModelLocation = config.getString("wsd.model_location")

  implicit val commandsRead: scopt.Read[Command.Value] =
    scopt.Read.reads(Command withName)

  implicit val relatedTypeRead: scopt.Read[RelatedType.Value] =
    scopt.Read.reads(RelatedType withName)

  implicit val modelWSDConfigRead: scopt.Read[WSDModel] =
    scopt.Read.reads(WSDModel.parseFromString)

  implicit val modelSenseVectorConfigRead: scopt.Read[SenseVectorModel] =
    scopt.Read.reads(SenseVectorModel.parseFromString)

  implicit val inventoryNameRead: scopt.Read[Sense.InventoryName] =
    scopt.Read.reads(Sense withName)

  val parser = new scopt.OptionParser[Params]("wsd-common") {
    head("wsp-common", "0.3.x")

    note("In many of the following operations two sense inventories are used and mapped against each other: " +
      "On the one side the data set with its sense intenvory, also called external or gold inventory, and on the " +
      "other side the internal sense inventory, which is stored in the db and depends on the model.\n")

    cmd("predict").action((_, c) => c.copy(mode = predict)).
      text(s"Run prediction for data set, output will be saved to: $defaultResultLocation").
      children(
        opt[WSDModel]('n', "modelName").required()
          .action((x, c) => c.copy(modelConfig = Some(x)))
          .text(s"Name of the model to use"),
        opt[String]('i', "inputFile").required()
          .action((x, c) => c.copy(inputFile = x))
          .text(s"Data set file to use, default see note below."),
        opt[RelatedType.Value]('r', "relatedType")
          .action((x, c) => c.copy(relatedType = x))
          .text(s"With what to fill the related column (${RelatedType.values.mkString(", ")}), default: ${RelatedType.hypernyms}"),
        opt[Unit]("useOnlyDirectDepWords").abbr("dd")
          .action((_, c) => c.copy(useOnlyDirectDepWords = true))
          .text(s"If set only directly dependent words on or from the target words are used as context features." +
            s"This requires the dataset to have a column with 'target_holing_features', such as LEFEX provides. https://github.com/tudarmstadt-lt/lefex."),
        opt[Unit]("skipOnWordFileExists").abbr("skip")
          .action((_, c) => c.copy(skipOnWordFileExists = true))
          .text("To speed up a rerun, skip for all already predicted words by checking if file exists."),
        opt[Unit]("enableRejectOption").abbr("reject")
          .action((_, c) => c.copy(rejectOption = true))
          .text("If the score is zero, reject prediction."),
        opt[Unit]("useNumIDs").abbr("num")
          .action((_, c) => c.copy(useNumIDs = true))
          .text("Use numeric IDs, as for TWSI"),
        //opt[Unit]('f', "filterSensesOnlyMapped")
        //  .action( (_, c) => c.copy(filterSensesOnlyMapped = true) )
        //  .text("Only use mapped senses."),
        opt[String]('s', "senseInventoryFile")
          .action((x, c) => c.copy(senseInventoryFile = Some(x)))
          .text(s"Name of the reference sense inventory only to use for filtering.")
      )

    cmd("evalbabel").action((_, c) => c.copy(mode = evalbabel)).
      text("").
      children(
        opt[WSDModel]('n', "modelName").required()
          .action((x, c) => c.copy(modelConfig = Some(x)))
          .text(s"Name of the model to use"),
        opt[String]('i', "inputFile").required()
          .action((x, c) => c.copy(inputFile = x))
          .text(s"Name of the data set file to use"),
        opt[String]('s', "senseInventoryFile").required()
          .action((x, c) => c.copy(senseInventoryFile = Some(x)))
          .text(s"Name of the reference sense inventory for the dataset")
      )

    cmd("exporttwsiinventory").action((_, c) => c.copy(mode = exporttwsiinventory)).
      children(
        opt[WSDModel]('n', "modelName").required()
          .action((x, c) => c.copy(modelConfig = Some(x)))
          .text(s"Name of the model to use")
      )

    cmd("exportinventory").action((_, c) => c.copy(mode = exportinventory)).
      text("This will export the default internal inventory optionally for words in the external reference inventory.").
      children(
        opt[String]('s', "externalReferenceInventory")
          .action((x, c) => c.copy(senseInventoryFile = Some(x)))
          .text(s"If an exteranl reference inventory is given, internal senses will be retreived for all words appearing in this reference inventory"),
        opt[Sense.InventoryName]('n', "inventoryName")
          .action((x, c) => c.copy(inventoryName = x))
          .text(s"The name of the inventory (${Sense.values.mkString(", ")}), default is: traditional"),
        opt[String]('o', "outputFolder")
          .action((x, c) => c.copy(outputFolder = x))
          .text(s"Folder to output the inventory to, default is: $defaultResultLocation"),
        opt[Unit]('f', "filterSensesOnlyMapped")
          .action( (_, c) => c.copy(filterSensesOnlyMapped = true) )
          .text("Only export mapped senses.")
      )

    cmd("filterdataset").action((_, c) => c.copy(mode = filterdataset)).
      text("This will remove rows with targets where senses are missing in the (internal) inventory.").
      children(
        opt[String]('i', "inputFile").required()
          .action((x, c) => c.copy(inputFile = x))
          .text(s"Name of the data set file to use"),
        opt[String]('s', "senseInventoryFile").required()
          .action((x, c) => c.copy(senseInventoryFile = Some(x)))
          .text(s"Name of the reference sense inventory for the dataset"),
        opt[String]('o', "outputFolder").required()
          .action((x, c) => c.copy(outputFolder = x))
          .text(s"Folder to output the new dataset without unpredictables"),
        opt[Unit]("removeUnpredictables")
          .action((_, c) => c.copy(filterSensesOnlyMapped = true))
          .text("Remove also rows with unpredictable gold (external) senses, i.e. unmapped senses.")
      )

    cmd("extractinventory").action((_, c) => c.copy(mode = extractinventory)).
      text("This will extract the external inventory from the dataset file.").
      children(
        opt[String]('i', "inputFile").required()
          .action((x, c) => c.copy(inputFile = x))
          .text(s"Name of the data set file to use"),
        opt[String]('o', "outputFolder")
          .action((x, c) => c.copy(outputFolder = x))
          .text(s"Folder to output the new inventory to, default is: $defaultResultLocation")
      )

    cmd("inventorystats").action((_, c) => c.copy(mode = inventorystats)).
      text("Calculate statistics for inventory file.").
      children(
        opt[String]('s', "senseInventoryFile").required()
          .action((x, c) => c.copy(senseInventoryFile = Some(x)))
          .text(s"Name of the inventory file to use"),
        opt[String]('o', "outputFolder")
          .action((x, c) => c.copy(outputFolder = x))
          .text(s"Folder to output the inventory statistics to, default is: $defaultResultLocation")
      )

    cmd("mappingstats").action((_, c) => c.copy(mode = mappingstats)).
      text("Calculate statistics for inventory mapping.").
      children(
        opt[String]('i', "internalInventoryFile").required()
          .action((x, c) => c.copy(senseInventoryFile = Some(x)))
          .text(s"Name of the internal inventory file to use"),
        opt[String]('e', "externalInventoryFile").required()
          .action((x, c) => c.copy(secondSenseInventoryFile = Some(x)))
          .text(s"Name of the external inventory file to use"),
        opt[String]('o', "outputFolder")
          .action((x, c) => c.copy(outputFolder = x))
          .text(s"Folder to output the inventory statistics to, default is: $defaultResultLocation"),
        opt[Unit]('g', "firstInventoryIsGlobal")
          .action((_, c) => c.copy(useGlobalInventory = true))
          .text(s"Wether the internal inventory is global (i.e. Co-Set")
      )

    cmd("evalstats").action((_, c) => c.copy(mode = evalstats)).
      text("Calculate statistics for evaluation file.").
      children(
        opt[String]('i', "inputFile").required()
          .action((x, c) => c.copy(inputFile = x))
          .text(s"Name of the data set file to use"),
        opt[String]('o', "outputFolder")
          .action((x, c) => c.copy(outputFolder = x))
          .text(s"Folder to output the evaulation dataset statistics to, default is: $defaultResultLocation")
      )

    cmd("indexdb").action((_, c) => c.copy(mode = indexdb)).
      text("Index the DB").
      children(
        opt[SenseVectorModel]('n', "modelName").required()
          .action((x, c) => c.copy(senseVectorModelConfig = Some(x)))
          .text(s"Name of the model to use")
      )

    cmd("downloadimages").action((_, c) => c.copy(mode = downloadimages)).
      text("Download images for cache from Bing").
      children(
        opt[String]('f', "frequencyFile").required()
          .action((x,c) => c.copy(frequencyFile = x))
          .text("A tab seperated CSV file with Sense IDs and their frequencies as columns."),
        opt[Int]('l', "limit").required()
          .action((x,c) => c.copy(downloadLimit = Some(x)))
          .text("Number of maximal downloads")
      )


    checkConfig {
      case x if x.mode == nocmd => failure("No command given.")
      case x if x.mode == predict
        && x.filterSensesOnlyMapped
        && x.senseInventoryFile.isEmpty => failure("senseInventoryFile must be given when filterSensesOnlyMapped is chosen.")
      case x if x.mode == predict
        && !x.filterSensesOnlyMapped
        && x.senseInventoryFile.nonEmpty => failure("senseInventoryFile must NOT be given when filterSensesOnlyMapped is NOT chosen.")
      case x if x.mode == exportinventory
        && x.filterSensesOnlyMapped
        && x.senseInventoryFile.isEmpty => failure("filterSensesOnlyMapped needs externalReferenceInventory")
      case _ => success
    }
  }

  def mainDownloadImages(frequencyFile: String, maxOpt: Option[Int]): Unit = {
    val max = maxOpt.getOrElse(10)

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val wsClient = AhcWSClient()

    val (_, wordFreqs) = TSVUtils.readWithHeaders(frequencyFile)
    val mostFreq = wordFreqs.sortBy(-_("freq").toInt).map(_("word")).take(max)

    println(s"STARTING DOWNLOADS (all senses for maximal $max words)\n")

    val downloader = new BingImageDownloader()

    val senses = mostFreq.toStream.flatMap(word => Sense.findAllByCaseIgnoredWord(word))
    val processed = senses.map { sense =>

      println(s"Checking image for ${sense.uniqueID}.")

      val check = downloader.readFromDiskOrDownload(sense)

      import scala.concurrent.ExecutionContext.Implicits.global
      check.onComplete {
        case Success(url) => println(s"Successfully ensured image for ${sense.uniqueID} exists.")
        case Failure(exception) => println(s"Error for ${sense.uniqueID}: $exception\n")
      }

      check

    } take max takeWhile { check =>
      import scala.concurrent.duration._
      // https://stackoverflow.com/a/21417522
      Await.result(check, 1 minutes).nonEmpty
    } length

    println(s"$processed senses processed")

    wsClient.close()
    system.terminate()
  }

  def main(args: Array[String]): Unit = {

    parser.parse(args, Params()) match {
      case Some(params) =>
        params.mode match {
          case `predict` => mainPrediction(
            dataSetFile = params.inputFile,
            modelConfig = params.modelConfig.get,
            useNumIDs = params.useNumIDs,
            inventory = params.senseInventoryFile,
            useOnlyDirectDepWords = params.useOnlyDirectDepWords,
            skipOnWordFileExists = params.skipOnWordFileExists,
            relatedType = params.relatedType,
            rejectOption = params.rejectOption
          )
          //case `indexdb` => mainIndexDB(params.modelConfig.get) // FIXME
          case `exporttwsiinventory` => mainExportTWSIInventory(params)
          case `evalbabel` => mainEvalBabel(params)
          case `filterdataset` => mainFilterDataSet(params)
          case `extractinventory` => mainExtractInventory(params)
          case `inventorystats` => mainInventoryStats(params)
          case `evalstats` => mainEvalStats(params)
          case `exportinventory` => mainExportDefaultInventory(params)
          case `mappingstats` => mainMappingStats(params)
          case `downloadimages` => mainDownloadImages(params.frequencyFile, params.downloadLimit)
        }
      case None =>
       // Utils.printConfig(config) // TODO why is printing even for common/run
      // arguments are bad, error message will be displayed by scopt
    }
  }

  def mainExportDefaultInventory(params: Params): Unit = {
    val outputDir = params.outputFolder

    val withReferenceInventory = params.senseInventoryFile.nonEmpty

    if (withReferenceInventory)
      logger.info(s"Exporting inventory for reference inventory: ${params.senseInventoryFile.get}")
    else
      logger.info(s"Exporting complete inventory")
    logger.info(s"Using output directory: $outputDir")

    val outputInventory = if (withReferenceInventory) {
      val referenceInventoryPath = params.senseInventoryFile.get
      val filteredName = if (params.filterSensesOnlyMapped) "without-unpredictables" else "without-missing"
      val inventoryConfig = detectInventoryConfig(referenceInventoryPath).toMap
      val mappgingName = if(inventoryConfig("mapping type") == "hyperhypernyms") "-with-hyperhypernyms" else ""
      s"$outputDir/Inventory-${params.inventoryName.toString.capitalize}$mappgingName-$filteredName.csv"
    } else {
      s"$outputDir/Inventory-${params.inventoryName.toString.capitalize}.csv"
    }

    val senses = if (withReferenceInventory) {
      val referenceInventoryPath = params.senseInventoryFile.get
      val (_, instances) = TSVUtils.readWithHeaders(referenceInventoryPath)
      val reference = instances.toList

      //assert(params.filterSensesOnlyMapped, "Non traditional inventories are always global, filtering for missing does not make sense.")

      val optWordFilter = if (params.inventoryName == Sense.traditional) {
        val words = reference.groupBy(s => s("word")).keySet
        (s: Sense) => words.contains(s.word.toLowerCase)
      } else {
        (s: Sense) => true
      }

      val modelConfig = params.modelConfig.get

      val mappingFilter = if (params.filterSensesOnlyMapped) {
        val mapping = SenseInventoryMapping.loadFromTSVForModel(referenceInventoryPath, modelConfig)
        (s: Sense) => mapping.isInternalMapped(s.sense_id)
      } else {
        (s: Sense) => true
      }

      Sense.findAll().filter(optWordFilter).filter(mappingFilter)

    } else {
      Sense.findAll()
    }

    val rows = senses.map(sense =>
      Map(
        "word" -> sense.word.toLowerCase(),
        "id" -> s"${sense.sense_id}",
        "related_words" -> sense.hypernyms.mkString(",")
      )
    )

    TSVUtils.writeWithHeaders(outputInventory, rows, List("word", "id", "related_words"))

    logger.info(s"Sense inventory exported to: $outputInventory")
  }

  def mainMappingStats(params: Params): Unit = {
    val internalPath = params.senseInventoryFile.get
    val externalPath = params.secondSenseInventoryFile.get
    val internalIsGlobal = params.useGlobalInventory

    val mapping = SenseInventoryMapping.loadFromTSVFiles(internalPath, externalPath, internalIsGlobal)

    val internalStats = mapping.internalInventoryByID.values.toList.map(i => Map(
      "id" -> i.id,
      "word" -> i.word,
      "num hypernyms" -> i.hypernyms.length.toString,
      "num mappings" -> mapping.numMappingsForInternal(i.id).toString
    ))

    val externalStats = mapping.externalInventoryByID.values.toList.map(e => Map(
      "id" -> e.id,
      "word" -> e.word,
      "num hypernyms" -> e.hypernyms.length.toString,
      "num mappings" -> mapping.numMappingsForExternal(e.id).toString
    ))

    val baseInternal = FilenameUtils.removeExtension(FilenameUtils.getBaseName(internalPath))
    val baseExternal = FilenameUtils.removeExtension(FilenameUtils.getBaseName(externalPath))
    val internalOutput = s"${params.outputFolder}/Mapping-$baseInternal-$baseExternal-Stats.csv"

    TSVUtils.writeWithHeaders(
      internalOutput,
      internalStats,
      List("id", "word", "num hypernyms", "num mappings")
    )
    logger.info(s"Internal mapping stats written to: $internalOutput")

    val externalOutput = s"${params.outputFolder}/Mapping-$baseExternal-$baseInternal-Stats.csv"
    TSVUtils.writeWithHeaders(
      externalOutput,
      externalStats,
      List("id", "word", "num hypernyms", "num mappings")
    )
    logger.info(s"External mapping stats written to: $externalOutput")
  }

  def mainFilterDataSet(params: Params): Unit = {
    val dataSetFile = params.inputFile
    val outputDir = params.outputFolder
    val baseDataSetFileName = FilenameUtils.removeExtension(FilenameUtils.getBaseName(dataSetFile))
    val filteredName = if (params.filterSensesOnlyMapped) "without-unpredictables" else "without-missing"
    val withoutUnpredictablesFile = s"$outputDir/$baseDataSetFileName-$filteredName.csv"
    val inventoryFile = params.senseInventoryFile.get

    logger.info(s"Reading dataset from: $dataSetFile")
    logger.info(s"Reading inventory from: $inventoryFile")
    logger.info(s"Using output directory: $outputDir")

    if (detectDatasetConfig(dataSetFile) != detectDatasetConfig(inventoryFile))
      logger.warn("Data set and inventory name seem not to have been generated with same config!")

    val (headers, instances) = TSVUtils.readWithHeaders(dataSetFile)

    val filtered = if (params.filterSensesOnlyMapped) {
      val mapping = SenseInventoryMapping.loadFromTSVForModel(inventoryFile, params.modelConfig.get)
      instances.toList.par.filter { i =>
        mapping.isExternalMapped(i("gold_sense_ids"))
      }
    } else {
      val rows = instances.toList
      val lookupExists = rows.groupBy(_ ("target")).keys.map { target =>
        target -> Sense.findAllByCaseIgnoredWord(target).nonEmpty
      }.toMap
      rows.filter { i => lookupExists(i("target")) }
    }

    TSVUtils.writeWithHeaders(
      withoutUnpredictablesFile,
      filtered.toList,
      headers
    )

    logger.info(s"New dataset without unpredictables written to: $withoutUnpredictablesFile")
  }

  def mainExportTWSIInventory(params: Params): Unit = {

    val output = s"$defaultResultLocation/twsi-${params.modelConfig}-inventory.csv"
    // FIXME PredictionSuite.exportInventory(output, params.modelConfig.get)
  }

  /*
  def detectConfigFromFilename(filename: String): List[(String, String)] = {
    detectRunConfig(filename) ::: detectDatasetConfig(filename)
  }

  def detectRunConfig(filename: String): List[(String, String)] = {
    // FIXME val model = filename.split("-").filter(PredictionModel.values.map(_.toString).contains(_)).head
    // FIXME val modelValue = PredictionModel.withName(model)
    // FIXME val dbModel = PredictionModel.getDBModelForModel(modelValue)
    List(
      ("reject option", if (filename contains "-reject") "true" else "false"),
      ("model", model),
      ("sense features", PredictionModel.getSenseFeatureTypeForModel(modelValue).toString),
      ("inventory", model match {
        case x if x.startsWith("cosets2k") => "cosets2k"
        case x if x.startsWith("cosets") => "cosets"
        case _ => "traditional"
      })
    )
  }
  */

  def detectDatasetConfig(filename: String): List[(String, String)] = {
    List(
      ("context features", if (filename contains "-with-deps") "depwords" else "words")
    ) ::: detectInventoryConfig(filename)
  }

  def detectInventoryConfig(filename: String): List[(String, String)] = {
    List(
      ("mapping type", if (filename contains "-with-hyperhypernyms") "hyperhypernyms" else "hypernyms"),
      ("inventory filter",
        if (filename contains "-without-unpredictables")
          "unpredictables"
        else if (filename contains "-without-missing")
          "missing"
        else
          "none"
        )
    )
  }

  def mainEvalBabel(params: Params): Unit = {
    val inputFile = params.inputFile
    val baseFileName = FilenameUtils.removeExtension(inputFile)
    val statFile = s"$baseFileName-stat.csv"
    val inventoryFile = params.senseInventoryFile.get
    val baseInventoryFilenName = FilenameUtils.removeExtension(FilenameUtils.getBaseName(inventoryFile))

    logger.info(s"Reading predicted dataset from: $inputFile")
    logger.info(s"Reading inventory from: $inventoryFile")

    if (detectDatasetConfig(inputFile) != detectDatasetConfig(inventoryFile))
      logger.warn("Data set and inventory name seem not to have been generated with same config!")

    val predictions = Evaluator.eval(
      inputFile,
      inventoryFile,
      params.modelConfig.get
    )

    val evaluationFile = s"$baseFileName-evaluated.csv"
    logger.info(s"Writing evaluated dataset to: $evaluationFile")
    TSVUtils.writeWithHeaders(
      evaluationFile,
      predictions,
      TSVUtils.readHeaders(inputFile) ::: List("correct", "predict_related_cleaned", "golden_related_cleaned")
    )
  }

  def mainExtractInventory(params: Params): Unit = {
    val dataSetPath = params.inputFile
    val baseDataSetFile = FilenameUtils.removeExtension(FilenameUtils.getBaseName(dataSetPath))
    val newInventoryPath = s"${params.outputFolder}/Inventory-${baseDataSetFile.stripPrefix("Dataset-")}.csv"

    logger.info(s"Reading dataset from: $dataSetPath")
    logger.info(s"Using output folder: ${params.outputFolder}")

    val (headers, rows) = TSVUtils.readWithHeaders(params.inputFile)
    val dataset = rows.toList

    val inventory = dataset.groupBy(r => (r("target"), r("gold_sense_ids"))).map { case ((target, id), first :: end) =>
      Map(
        "word" -> target,
        "id" -> id,
        "related_words" -> first("golden_related")
      )
    }

    TSVUtils.writeWithHeaders(
      newInventoryPath,
      inventory.toList,
      List("word", "id", "related_words")
    )

    logger.info(s"Inventory written to: $newInventoryPath")
  }

  def mainInventoryStats(params: Params): Unit = {

    val inventoryPath = params.senseInventoryFile.get
    val baseInventoryFile = FilenameUtils.removeExtension(FilenameUtils.getBaseName(inventoryPath))

    val statsPath = s"${params.outputFolder}/$baseInventoryFile-stats.csv"

    logger.info(s"Reading inventory from: $inventoryPath")
    logger.info(s"Using output folder: ${params.outputFolder}")

    val (_, rows) = TSVUtils.readWithHeaders(inventoryPath)
    val senses = rows.toList

    val sensesPerWords = senses.groupBy(s => s("word")).mapValues(_.length)

    val statList = List(
      ("inventory name", baseInventoryFile.stripPrefix("Inventory-").split("-").head.toLowerCase()),
      ("senses per word", s"${Utils.avg(sensesPerWords.values.toList)}"),
      ("num senses", s"${senses.length}"),
      ("num words", s"${sensesPerWords.size}")
    ) ::: detectInventoryConfig(inventoryPath)

    logger.info(s"Writing stats to: $statsPath")
    TSVUtils.writeWithHeaders(
      statsPath,
      List(statList.toMap),
      statList.toMap.keys.toList
    )

    /*
    val inventoryMappingFile = s"$defaultResultLocation/$baseInventoryFilenName-mapping.csv"
    logger.info(s"Writing inventory mapping to: $inventoryMappingFile")
    TSVUtils.writeWithHeaders(
      inventoryMappingFile,
      stats.mapping.internalMappingProjection.toList,
      List("internal_id", "external_ids")
    )
    */
  }

  def mainIndexDB(): Unit = {

    //CREATE TABLE sense_num_ids (id TEXT, num_id SERIAL);
    // INSERT INTO sense_num_ids (id) SELECT id FROM sense_clusters;
    //CREATE INDEX ON sense_num_ids (id);

    DB autoCommit { implicit session =>
      sql"""
        CREATE INDEX ON senses (inventory, sense_id);
        CREATE INDEX ON senses (inventory, lower(word));
        CREATE INDEX ON sense_vectors (model, sense_id);
        CREATE INDEX ON word_vectors (model, word);
        CREATE INDEX ON word_vector_feature_indices (model, index);
        """.execute.apply()
    }
  }

  def mainPrediction(
                      dataSetFile: String,
                      modelConfig: WSDModel,
                      useNumIDs: Boolean,
                      inventory: Option[String],
                      useOnlyDirectDepWords: Boolean,
                      skipOnWordFileExists: Boolean,
                      relatedType: RelatedType.Value,
                      rejectOption: Boolean
                    ): Unit = {
    logger.info(s"Using data set: $dataSetFile")
    val filteredPostfix = inventory.map(x => "-filtered").getOrElse("")
    val rejectPostfix = if (rejectOption) "-reject" else ""
    val baseName = FilenameUtils.removeExtension(FilenameUtils.getBaseName(dataSetFile))
    val resultLocation = s"$defaultResultLocation/$baseName-$modelConfig$filteredPostfix$rejectPostfix"

    logger.info(s"Using result location: $resultLocation")
    logger.info(inventory.map(x => s"Filtering with sense inventory: $x").getOrElse("Not filtering senses!"))
    logger.info(if (rejectOption) "Reject option is enabled!" else "Disable reject option.")

    FileUtils.ensureFolderExists(resultLocation)

    val runConfig = RunConfig(
      inventory,
      onlyDirectDepFeatures = useOnlyDirectDepWords,
      skipOnWordFileExits = skipOnWordFileExists,
      rejectOption = rejectOption,
      useNumIDs = useNumIDs,
      relatedType = relatedType
    )
    new DatasetPredictionPipeline(modelConfig, runConfig)
      .predictDatasetFileWithAllWords(dataSetFile, resultLocation)

    val files = new File(resultLocation).listFiles.filter(_.isFile).toList

    val predictedFile = s"$resultLocation-predicted.csv"

    val rows = files.map(f => TSVUtils.readWithHeaders(f.getCanonicalPath)).flatMap(_._2.toList)
    files.headOption match {
      case Some(f) => TSVUtils.writeWithHeaders(
        predictedFile,
        rows,
        TSVUtils.readHeaders(f.getCanonicalPath)
      )
      case None =>
        TSVUtils.write(predictedFile, List.empty[List[String]])
    }
    logger.info(s"Final result written to: $predictedFile")

  }

  def mainEvalStats(params: Params) {
    val dataSetPath = params.inputFile

    logger.info(s"Reading dataset from: $dataSetPath")
    logger.info(s"Using output folder: ${params.outputFolder}")

    val stats = EvaluationStatistics.create(dataSetPath)

    val basedataSetFile = FilenameUtils.getBaseName(dataSetPath)

    val statList = List(
      ("file", basedataSetFile),
      ("model", s"${params.modelConfig}")
    ) // FIXME ::: detectConfigFromFilename(dataSetPath) ::: stats.toList

    val statsPath = s"${params.outputFolder}/${FilenameUtils.removeExtension(basedataSetFile)}-stats.csv"

    logger.info(s"Writing stats to: $statsPath")
    TSVUtils.writeWithHeaders(
      statsPath,
      List(statList.toMap),
      statList.toMap.keys.toList
    )
  }

  def mainPipeline(params: Params): Unit = {

  }


}
