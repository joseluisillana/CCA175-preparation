package com.sparkscala.part1

import org.apache.hadoop.io._
import org.apache.hadoop.mapreduce.lib.output._
import org.apache.spark.{SparkConf, SparkContext}

object ReadAndWriteToHDFSinSFNewApi {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)

    val dataRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/departments_jl")

    val dataMap = dataRDD.map(x => (new Text(x.split(",")(0)), new Text(x.split(",")(1))))

    val path = "/user/joseluisillana1709/pruebas_spark/scalaspark/sparkresults/departmentsSequenceFileNewApi"

    dataMap.saveAsNewAPIHadoopFile(
      path,
      classOf[Text],
      classOf[Text],
      classOf[SequenceFileOutputFormat[Text ,Text ]]
    )
  }
}
