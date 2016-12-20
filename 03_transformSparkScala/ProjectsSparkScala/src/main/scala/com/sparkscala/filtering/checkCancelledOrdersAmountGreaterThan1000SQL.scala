package com.sparkscala.filtering

import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}


object checkCancelledOrdersAmountGreaterThan1000SQL {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)
    val sqlContext = new HiveContext(sc)

    sqlContext.sql("select * from " +
      "(select o.order_id, " +
      "sum(oi.order_item_subtotal) as order_item_revenue " +
      "from orders_jl o join order_items_jl oi " +
      "on o.order_id = oi.order_item_order_id " +
      "where o.order_status = 'CANCELED' " +
      "group by o.order_id) q " +
      "where order_item_revenue >= 1000")
      .count()
  }
}
