package com.sparkscala.aggtotals

import org.apache.spark.sql._
import org.apache.spark.{SparkConf, SparkContext}


object AggAvgTotalRevenueOrderItems {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)


    val orderItemsRDD = sc.
      textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/order_items_jl")

    val revenue = orderItemsRDD.map(x => x.split(",")(4).toDouble).reduce((acc,value) => acc + value)
    val totalOrders = orderItemsRDD.map(x => x.split(",")(1).toInt).distinct().count()


     println(s"AVG of total revenues of order items : ${revenue / totalOrders}")
  }
}
