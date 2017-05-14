package de.tudarmstadt.lt.wsd.common.utils

import skinny.orm.SkinnyMapperWithId

/**
  * Created by fide on 08.04.17.
  */
trait SkinnyMapperWithStringId[T] extends SkinnyMapperWithId[String, T] {
  override def idToRawValue(id: String): Any = id
  override def rawValueToId(rawValue: Any): String = rawValue.toString
}
