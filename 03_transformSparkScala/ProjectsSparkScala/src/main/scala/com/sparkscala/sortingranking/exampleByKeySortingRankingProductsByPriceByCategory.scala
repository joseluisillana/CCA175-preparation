package com.sparkscala.sortingranking

import org.apache.spark.{SparkConf, SparkContext}


object exampleByKeySortingRankingProductsByPriceByCategory {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)


    val products = sc.
      textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/products_jl")

    val productsMap = products.
      map(rec => (rec.split(",")(1), rec))

    val productsGroupBy = productsMap.
      groupByKey()

    productsGroupBy.
      take(20).
      foreach(el => println(s"Without sorting: {el}"))

    val productsGroupBySorted = productsGroupBy.map(rec => rec._2.toList.sortBy(k => k.split(",")(4).toFloat)
      ).take(100).foreach(el => println(s"With sorting ASC: {el}"))

    val productsGroupBySortedDesc = productsGroupBy.
      map(rec => (
        rec._2.toList.sortBy(k => -k.split(",")(4).toFloat))
      ).
      take(100).
      foreach(el => println(s"With sorting DESC: {el}"))

    val productsGroupBySortedDescFM  = productsGroupBy.
      flatMap(rec => (
        rec._2.toList.sortBy(k => -k.split(",")(4).toFloat))
      ).
      take(100).
      foreach(el => println(s"With sorting DESC FlatMap: {el}"))
  }
}
