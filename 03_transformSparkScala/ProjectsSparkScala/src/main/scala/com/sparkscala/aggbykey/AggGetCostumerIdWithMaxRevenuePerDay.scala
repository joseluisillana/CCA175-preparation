package com.sparkscala.aggbykey

import org.apache.spark.{SparkConf, SparkContext}


object AggGetCostumerIdWithMaxRevenuePerDay {
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


    val ordersPerDayPerCustomer = orderJoinOrderItems.
      map(rec => (
        (rec._2._2.split(",")(1), rec._2._2.split(",")(2)),
        rec._2._1.split(",")(4).toFloat)
      )

    val revenuePerDayPerCustomer = ordersPerDayPerCustomer.reduceByKey((acum, value) => acum + value)


    val revenuePerDayPerCustomerMap = revenuePerDayPerCustomer.
      map(rec => (rec._1._1, (rec._1._2, rec._2)))
    val topCustomerPerDaybyRevenue = revenuePerDayPerCustomerMap.
      reduceByKey((x, y) => (if(x._2 >= y._2) x else y))


    // Using regular function
    def findMax(x: (String, Float), y: (String, Float)): (String, Float) = {
      if(x._2 >= y._2)
        return x
      else
        return y
    }

    val topCustomerPerDaybyRevenueFunc = revenuePerDayPerCustomerMap.
      reduceByKey((x, y) => findMax(x, y))

    topCustomerPerDaybyRevenue.take(5).foreach(el =>
      println(s"Costumer with Max Revenue per day: ${el}"))

    topCustomerPerDaybyRevenueFunc.take(5).foreach(el =>
      println(s"FUNK Costumer with Max Revenue per day: ${el}"))
  }
}
