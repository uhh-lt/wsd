package de.tudarmstadt.lt.wsd.common.utils

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.slf4j.LazyLogging

object DBUtils extends LazyLogging{

  private val config = ConfigFactory.load()

  val jdbcUrl: String = {
    val baseUrl = config.getString(s"db.default.url")
    val user = config.getString(s"db.default.user")
    val pass = config.getString(s"db.default.password")

    s"$baseUrl?user=$user&password=$pass"
  }

  val driver: String = config.getString(s"db.default.driver")

  val combatBatchSizeProps = new java.util.Properties()
  combatBatchSizeProps.setProperty("batchsize", "1")
  combatBatchSizeProps.setProperty("driver", driver)

  val emptyProps = new java.util.Properties()
  emptyProps.setProperty("driver", driver)

}