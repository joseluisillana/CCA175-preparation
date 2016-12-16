package com.sparkscala.aggtotals

import org.apache.spark.sql._
import org.apache.spark.{SparkConf, SparkContext}


object AggTotalRevenueOrdersItems {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)


    val orderItemsRDD = sc.
      textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/order_items_jl")

    val orderItemsMap = orderItemsRDD.map(x => (x.split(",")(4).toDouble))

    val orderItemReduce = orderItemsMap.reduce((acc,value) => acc + value)

     println(s"TOTAL of revenues : ${orderItemReduce}")
  }
}
