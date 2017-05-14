package de.tudarmstadt.lt.wsd.pipeline.feature

import org.apache.spark.ml.UnaryTransformer
import org.apache.spark.sql.types.DataType

class UnaryUDFTransformer[T, U](override val uid: String, f: T => U, inType: DataType, outType: DataType)
  extends UnaryTransformer[T, U, UnaryUDFTransformer[T, U]] {

  override protected def createTransformFunc: T => U = f

  override protected def validateInputType(inputType: DataType): Unit =
    require(inputType == inType)

  override protected def outputDataType: DataType = outType
}

