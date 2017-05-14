package de.tudarmstadt.lt.wsd.pipeline.sql

/**
  * Created by fide on 07.09.16.
  */
object functions {
  def vector_sum = new VectorSumUDAF

  def norm_vec_sum = new NormalizedVectorSumUDAF
}
