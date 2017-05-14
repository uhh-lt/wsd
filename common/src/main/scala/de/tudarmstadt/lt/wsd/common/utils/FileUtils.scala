package de.tudarmstadt.lt.wsd.common.utils

import java.io.{File, PrintWriter}

import org.apache.commons.io.{FileUtils => IOFileUtils}
import org.apache.commons.io.FilenameUtils

import scala.reflect.io.FileOperationException;

/**
  * Created by fide on 16.12.16.
  */
object FileUtils {

  def readContent(filePath: String): String =  {
    scala.io.Source.fromFile(filePath).mkString
  }

  def writeContent(filePath: String, content: String) = {
    val pw = new PrintWriter(new File(filePath))
    pw.write(content)
    pw.close()
  }

  def ensureFolderExists(path: String): Unit = {
    val folder = new File(path)
    if (!folder.exists()) {
      val created = folder.mkdirs()
      if (!created) throw FileOperationException(s"Could not create directory $path")
    }
  }

  def deleteFolderIfExists(path: String) = {
    val folder = new File(path)
    if (folder.exists()) {
      IOFileUtils.deleteDirectory(folder)
    }
  }

  def humanFileSize(path: String) = {
    val bytes = IOFileUtils.sizeOf(new File(path))
    IOFileUtils.byteCountToDisplaySize(bytes)
  }

  def existsFile(path: String) =  {
    new java.io.File(path).exists
  }


}
