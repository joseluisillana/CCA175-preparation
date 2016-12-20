package com.sparkscala.filtering

import org.apache.spark.{SparkConf, SparkContext}


object basicFilterOrdersByStatus {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)


    val ordersRDD = sc.
      textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")

    val ordersFilteredRDD = ordersRDD.filter(x => x.split(",")(4).equalsIgnoreCase("COMPLETE"))


    ordersFilteredRDD.take(5).foreach(el =>
      println(s"Basic filter or complete orders: ${el}"))
  }
}
