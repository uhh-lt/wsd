package de.tudarmstadt.lt.wsd.common.utils

import java.io.FileNotFoundException

import com.typesafe.scalalogging.LazyLogging
import com.typesafe.config.{Config, ConfigRenderOptions}

import scala.util.Random

object Utils extends LazyLogging {

  def printConfig(config: Config): Unit = {
    val renderOpts = ConfigRenderOptions.defaults()
      .setOriginComments(false)
      .setComments(false)
      .setJson(false)
    println(config.root().render(renderOpts))
  }


  // See: http://stackoverflow.com/a/4608061
  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def getResourcePath(path: String): String = {
    val resource = getClass.getResource(path)
    if (resource == null) throw new FileNotFoundException(path)
    resource.getPath
  }

  def generateRandomName(): Unit = {
    val alphabet = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ ("_")
    def generateName = (1 to 15).map(_ => alphabet(Random.nextInt(alphabet.size))).mkString
  }

  def avg(list: List[Int], excludeZero: Boolean = false): Double = {
    val isNotZeroIfExclude = (count: Int) => if (excludeZero) count != 0 else true
    val filteredList = list.filter(isNotZeroIfExclude)
    filteredList.sum.toDouble / filteredList.length
  }

  def time[R](block: => R, name: String): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    logger.warn(s"Profiling $name: elapsed time: ${t1 - t0} ns)")
    result
  }

}