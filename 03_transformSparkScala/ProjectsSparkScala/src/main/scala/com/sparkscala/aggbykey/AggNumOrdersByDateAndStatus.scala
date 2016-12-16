package com.sparkscala.aggbykey

import org.apache.spark.{SparkConf, SparkContext}


object AggNumOrdersByDateAndStatus {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)


    val ordersRDD = sc.
      textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")

    val ordersMap = ordersRDD.map(rec => ((rec.split(",")(1), rec.split(",")(3)), 1))

    val ordersReduced = ordersMap.reduceByKey((acum, value) => acum + value)


    ordersReduced.collect().foreach(el =>
     println(s"AVG by key count orders by status : ${el}"))
  }
}
