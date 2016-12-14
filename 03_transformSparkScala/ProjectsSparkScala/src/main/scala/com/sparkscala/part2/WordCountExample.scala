package com.sparkscala.part2

import org.apache.spark.{SparkConf, SparkContext}

object WordCountExample {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)

    val dataRDD = sc.textFile("/user/joseluisillana1709/davincibook")
    val dataflatMap = dataRDD.flatMap(x => x.split(" "))
    val dataMap = dataflatMap.map(x => (x,1))
    val reduceResult = dataMap.reduceByKey( (x,y) => x+y )

    reduceResult.saveAsTextFile("/user/joseluisillana1709/pruebas_spark/result/wordCountResult")
  }
}
