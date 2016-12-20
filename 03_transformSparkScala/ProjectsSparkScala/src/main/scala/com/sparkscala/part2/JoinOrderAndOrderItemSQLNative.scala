package com.sparkscala.part2

import org.apache.spark.sql._
import org.apache.spark.{SparkConf, SparkContext}

case class Orders(
  order_id: Int,
  order_date: String,
  order_customer_id: Int,
  order_status: String)

case class OrderItems(
  order_item_id: Int,
  order_item_order_id: Int,
  order_item_product_id: Int,
  order_item_quantity: Int,
  order_item_subtotal: Float,
  order_item_product_price: Float
)

object JoinOrderAndOrderItemSQLNative {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)

    val sqlContext = new SQLContext(sc)
    sqlContext.sql("set spark.sql.shuffle.partitions=10");

    val ordersRDD = sc.
      textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")

    val ordersMap = ordersRDD.
      map(o => o.split(","))



    val orders = ordersMap.map(o => Orders(o(0).toInt, o(1), o(2).toInt, o(3)))

    import sqlContext.createSchemaRDD

    orders.registerTempTable("orders")

    val orderItemsRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/order_items_jl")
    val orderItemsMap = orderItemsRDD.map(oi => oi.split(","))



    val orderItems = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/order_items_jl").
      map(rec => rec.split(",")).
      map(oi => OrderItems(oi(0).toInt, oi(1).toInt, oi(2).toInt, oi(3).toInt, oi(4).toFloat, oi(5).toFloat))

    orderItems.registerTempTable("order_items")

    val joinAggData = sqlContext.sql("select o.order_date, sum(oi.order_item_subtotal), " +
      "count(distinct o.order_id) from orders o join order_items oi " +
      "on o.order_id = oi.order_item_order_id " +
      "group by o.order_date order by o.order_date")

    joinAggData.collect().foreach(value => println(s"TOTAL of revenues and sales per day: ${value}"))
  }
}
