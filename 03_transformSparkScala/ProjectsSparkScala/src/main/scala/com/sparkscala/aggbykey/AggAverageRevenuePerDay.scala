package com.sparkscala.aggbykey

import org.apache.spark.{SparkConf, SparkContext}


object AggAverageRevenuePerDay {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)


    val ordersRDD = sc.
      textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")

    val orderItemsRDD = sc.
      textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/order_items_jl")

    // (1,  (1,2013-07-25 00:00:00.0,11599,CLOSED))
    val ordersMap = ordersRDD.map(rec => (rec.split(",")(0), rec))

    // (1, (1,1,957,1,299.98,299.98))
    val orderItemsMap = orderItemsRDD.map(rec => (rec.split(",")(1),rec))

    //[(String, (String, String))] =
    // (2828,
    //// (
    ////// (7097,2828,403,1,129.99,129.99),
    ////// (2828,2013-08-10 00:00:00.0,4952,SUSPECTED_FRAUD)
    //// )
    // )
    val orderJoinOrderItems = orderItemsMap.join(ordersMap)


    //[((String, String), Float)] =
    // (
    //// (
    ////// 2013-08-10 00:00:00.0,
    ////// 2828
    //// ),
    //// 129.99
    // )
    val orderJoinOrderItemsMap = orderJoinOrderItems.map(rec =>
      (
        (rec._2._2.split(",")(1), rec._1),
        rec._2._1.split(",")(4).toFloat
        ))

    val revenuePerDayPerOrder = orderJoinOrderItemsMap.reduceByKey((acum, value) => acum + value)

    //[(String, Float)] =
    // (
    //// 2013-09-30 00:00:00.0,
    //// 869.88995
    // )
    val revenuePerDayPerOrderMap = revenuePerDayPerOrder.map(rec => (rec._1._1, rec._2))

    val revenuePerDay = revenuePerDayPerOrderMap.aggregateByKey((0.0, 0))(
      (acc, revenue) => (acc._1 + revenue, acc._2 + 1),
      (total1, total2) => (total1._1 + total2._1, total2._2 + total2._2)
    )

    revenuePerDay.collect().foreach(el =>
     println(s"Revenue per day: ${el}"))

    val avgRevenuePerDay = revenuePerDay.
      map(x => (x._1, x._2._1/x._2._2))

    avgRevenuePerDay.collect().foreach(el =>
      println(s"AVG of Revenue per day: ${el}"))
  }
}
