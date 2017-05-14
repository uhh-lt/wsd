package de.tudarmstadt.lt.wsd.pipeline.utils

import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.SparkSession

/**
  * Created by fide on 12.09.16.
  */
trait RuntimeContext {

  val config = ConfigFactory.load()

  val warehouseLocation = "file:${system:user.dir}/spark-warehouse"

  lazy val spark = SparkSession
    .builder()
    .appName(config.getString("wsd.spark.application_name"))
    //.config("spark.eventLog.enabled", "true") // if enabled, make sure folder /tmp/spark-event exists
    //.config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    //.config("spark.sql.warehouse.dir", warehouseLocation)
    .enableHiveSupport()
    .getOrCreate()
}
