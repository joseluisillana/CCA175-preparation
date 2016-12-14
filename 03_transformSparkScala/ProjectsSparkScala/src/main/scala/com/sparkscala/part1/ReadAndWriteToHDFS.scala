package com.sparkscala.part1

import org.apache.spark.{SparkConf, SparkContext}

object ReadAndWriteToHDFS {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("ReadAndWriteToHDFS with spark and scala")
    val sc = new SparkContext(conf)
    val dataRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/departments_jl")

    dataRDD.collect().foreach(println)
    dataRDD.count()
    dataRDD.saveAsTextFile("/user/joseluisillana1709/pruebas_spark/sparkresults/departments")
  }
}
