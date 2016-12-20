package com.sparkscala.filtering

import org.apache.spark.{SparkConf, SparkContext}


object checkCancelledOrdersAmountGreaterThan1000 {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)


    val ordersRDD = sc.
      textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")


    val orderItemssRDD = sc.
      textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/order_items_jl")

    val ordersParsedRDD = ordersRDD.
      filter(x => x.split(",")(3).toLowerCase.contains("CANCELLED".toLowerCase)).
      map(rec => (rec.split(",")(0).toInt,rec))

    val orderItemsParsedRDD = orderItemssRDD.map(x => (x.split(",")(1).toInt, x.split(",")(4).toFloat))

    val orderItemAGG = orderItemsParsedRDD.reduceByKey((acum, value) => acum + value)

    val ordersJoinOrderItems = orderItemAGG.join(ordersParsedRDD)

    val ordersCancelledAmontGreaterThan = ordersJoinOrderItems.filter(x => x._2._1 >= 1000)

    ordersCancelledAmontGreaterThan.take(5).foreach(el =>
      println(s"Orders cancelled amount greater than 1000: ${el}"))
  }
}
