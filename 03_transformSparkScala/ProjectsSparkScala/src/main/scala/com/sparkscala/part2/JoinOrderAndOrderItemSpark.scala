package com.sparkscala.part2

import org.apache.spark.{SparkConf, SparkContext}

object JoinOrderAndOrderItemSpark {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)

    val dataOrdersRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")
    val dataOrderItemsRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/order_items_jl")

    val dataOrdersMap = dataOrdersRDD.map(line => (line.split(",")(0).toInt,line))
    val dataOrderItemsMap = dataOrderItemsRDD.map(line => (line.split(",")(1).toInt,line))

    val dataJoined = dataOrderItemsMap.join(dataOrdersMap)

    // Revenues per day (DATE,AMOUNT)
    val revenuePerOrderPerDay = dataJoined.map(t => (t._2._2.split(",")(1), t._2._1.split(",")(4).toFloat) )
    // Calculate TOTAL of revenues per day (DATE,TOTAL AMOUNT)
    val totalRevenuePerDay = revenuePerOrderPerDay.reduceByKey((total1, total2) => total1 + total2)

    // Orders per day (DATE,ORDER_ID)
    val ordersPerDay = dataJoined.map(rec => rec._2._2.split(",")(1) + "," + rec._1).distinct()
    // Calculate TOTAL of ORDERS PER DAY (DATE, TOTAL COUNT OF ORDERS)
    val totalOrdersPerDay = ordersPerDay.map(rec => (rec.split(",")(0), 1)).reduceByKey((acum, value) => acum + value)

    totalRevenuePerDay.sortByKey().collect().foreach(value => println(s"TOTAL of revenues per day: ${value}"))

    // RDD like (DATE, TOTAL COUNT OF ORDERS, TOTAL AMOUNT REVENUES)
    val finalJoinRDD = totalOrdersPerDay.
      join(totalRevenuePerDay)

    finalJoinRDD.collect().foreach(value => println(s"TOTAL of revenues and sales per day: ${value}"))
  }
}
