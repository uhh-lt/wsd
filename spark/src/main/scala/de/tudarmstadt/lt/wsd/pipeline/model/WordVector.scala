package de.tudarmstadt.lt.wsd.pipeline.model

import org.apache.spark.ml.linalg.Vector

/**
  * Created by fide on 07.04.17.
  */
case class WordVector(word: String, vector: Vector)
