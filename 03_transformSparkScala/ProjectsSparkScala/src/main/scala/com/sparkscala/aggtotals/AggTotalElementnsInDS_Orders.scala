package com.sparkscala.aggtotals

import org.apache.spark.sql._
import org.apache.spark.{SparkConf, SparkContext}


object AggTotalElementnsInDS_Orders {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)


    val ordersRDD = sc.
      textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")


     println(s"TOTAL of orders: ${ordersRDD.count()}")
  }
}
