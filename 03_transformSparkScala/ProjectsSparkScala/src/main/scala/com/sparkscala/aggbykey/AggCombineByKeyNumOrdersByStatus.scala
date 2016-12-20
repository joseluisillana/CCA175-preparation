package com.sparkscala.aggbykey

import org.apache.spark.{SparkConf, SparkContext}


object AggCombineByKeyNumOrdersByStatus {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)


    val orderRDD = sc.
      textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")

    val ordersMap = orderRDD.map(x => (x.split(",")(3),1))

    val ordersByStatusRDD = ordersMap.combineByKey(
      value => 1,
      (acc: Int, value: Int) => acc+1,
      (acc: Int, value: Int) => acc+value
    )

    ordersByStatusRDD.collect().foreach(el =>
     println(s"AVG by key count orders by status : ${el}"))
  }
}
