package com.sparkscala.sortingranking

import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}


object exampleByKeySortingRankingSparkSQL {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)

    //Sorting using queries
    // Global sorting and ranking
    val queryA = "select * from products order by product_price desc"
    val queryB = "select * from products order by product_price desc limit 10"

    //By key sorting
    //Using order by is not efficient, it serializes
    val queryC = "select * from products order by product_category_id, product_price desc"

    //Using distribute by sort by (to distribute sorting and scale it up)
    val queryD = "select * from products distribute by product_category_id sort by product_price desc"

    //By key ranking (in Hive we can use windowing/analytic functions)
    val queryE = "select * from (select p.*," +
    "dense_rank() over (partition by product_category_id order by product_price desc) dr " +
    "  from products p" +
    "distribute by product_category_id) q" +
    "where dr <= 2 order by product_category_id, dr"

    sqlContext.sql(queryA).take(5).foreach(el => println(s"A: {el}"))
    sqlContext.sql(queryB).take(5).foreach(el => println(s"B: {el}"))
    sqlContext.sql(queryC).take(5).foreach(el => println(s"C: {el}"))
    sqlContext.sql(queryD).take(5).foreach(el => println(s"D: {el}"))
    sqlContext.sql(queryE).take(5).foreach(el => println(s"E: {el}"))
  }
}
