package com.sparkscala.part2

import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}

object JoinOrderAndOrderItemHive {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)

    val sqlContext = new HiveContext(sc)
    sqlContext.sql("set spark.sql.shuffle.partitions=10");

    val joinAggData = sqlContext.sql("select " +
      "o.order_date, " +
      "round(sum(oi.order_item_subtotal), 2), " +
      "count(distinct o.order_id) " +
      "from retail_db_jlir.orders_jl o " +
      "join retail_db_jlir.order_items_jl oi " +
      "on o.order_id = oi.order_item_order_id " +
      "group by o.order_date " +
      "order by o.order_date")

    joinAggData.collect().foreach(value => println(s"TOTAL of revenues and sales per day: ${value}"))
  }
}
