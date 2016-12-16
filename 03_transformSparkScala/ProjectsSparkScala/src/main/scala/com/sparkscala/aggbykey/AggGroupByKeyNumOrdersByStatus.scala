package com.sparkscala.aggbykey

import org.apache.spark.{SparkConf, SparkContext}


object AggGroupByKeyNumOrdersByStatus {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)


    val orderRDD = sc.
      textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")

    val ordersMap = orderRDD.map(x => (x.split(",")(3),1))

    val ordersGrouped = ordersMap.groupByKey()

    val numOrdersByStatusRDD = ordersGrouped.map(x => (x._1, x._2.sum))

    numOrdersByStatusRDD.foreach(el =>
     println(s"AVG by key count orders by status : ${el}"))
  }
}
