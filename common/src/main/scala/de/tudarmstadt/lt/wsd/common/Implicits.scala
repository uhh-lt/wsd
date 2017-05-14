package de.tudarmstadt.lt.wsd.common

import de.tudarmstadt.lt.wsd.common.model.{SenseVectorModel, WordVector}

/**
  * Created by fide on 28.04.17.
  */

object Implicits {
  implicit def convertStringToSenseVectorModel: (String) => SenseVectorModel = SenseVectorModel.parseFromString
  implicit def convertStringToWordVectorModel: (String) => WordVector.ModelName = WordVector.withName
}
