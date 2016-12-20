package com.sparkscala.sortingranking

import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}


object exampleGlobalSortingRankingSort {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)


    val ordersRDD = sc.
      textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")

    ordersRDD.
      map(rec => (rec.split(",")(0).toInt, rec)).
      sortByKey().
      take(5).
      foreach(println)

    ordersRDD.
      map(rec => (rec.split(",")(0).toInt, rec)).
      sortByKey(false).
      take(5).
      foreach(println)

    ordersRDD.
      map(rec => (rec.split(",")(0).toInt, rec)).
      top(5).
      foreach(println)

  }
}
