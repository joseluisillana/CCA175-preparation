package com.sparkscala.sortingranking

import org.apache.spark.{SparkConf, SparkContext}


object exampleGlobalSortingRankingTakeOrdered {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)


    val ordersRDD = sc.
      textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")

    ordersRDD.map(rec => (rec.split(",")(0).toInt, rec)).
      takeOrdered(5).
      foreach(println)

    ordersRDD.map(rec => (rec.split(",")(0).toInt, rec)).
      takeOrdered(5)(Ordering[Int].reverse.on(x => x._1)).
      foreach(println)

    ordersRDD.
      takeOrdered(5)(
        Ordering[Int].on(x => x.split(",")(0).toInt)
      ).
      foreach(println)

    ordersRDD.
      takeOrdered(5)(
        Ordering[Int].reverse.on(x => x.split(",")(0).toInt)
      ).
      foreach(println)

  }
}
