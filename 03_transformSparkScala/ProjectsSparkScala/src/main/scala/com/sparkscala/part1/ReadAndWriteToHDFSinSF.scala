package com.sparkscala.part1

import org.apache.hadoop.io._
import org.apache.spark.{SparkConf, SparkContext}

object ReadAndWriteToHDFSinSF {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName(s"${this.getClass.getName}  with spark and scala")
    val sc = new SparkContext(conf)

    val dataRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/departments_jl")

    val dataMap = dataRDD.map(x => (NullWritable.get(),x))

    dataMap.saveAsSequenceFile("/user/joseluisillana1709/pruebas_spark/scalaspark/sparkresults/departmentsSequenceFile")
  }
}
