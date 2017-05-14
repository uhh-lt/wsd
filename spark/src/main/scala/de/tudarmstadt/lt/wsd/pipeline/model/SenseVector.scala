package de.tudarmstadt.lt.wsd.pipeline.model

import org.apache.spark.ml.linalg.Vector

case class SenseVector(sense_id: String, vector: Vector)
