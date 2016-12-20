package com.sparkscala.filtering

import org.apache.spark.{SparkConf, SparkContext}


object basicFilterOrdersByIdOder {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)


    val ordersRDD = sc.
      textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")

    val ordersFilteredRDD = ordersRDD.filter(x => x.split(",")(1).toInt > 100)


    ordersFilteredRDD.take(5).foreach(el =>
      println(s"Basic filter or complete orders: ${el}"))
  }
}
