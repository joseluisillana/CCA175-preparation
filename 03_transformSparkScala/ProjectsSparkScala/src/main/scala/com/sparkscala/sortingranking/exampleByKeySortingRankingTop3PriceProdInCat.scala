package com.sparkscala.sortingranking

import org.apache.spark.{SparkConf, SparkContext}


object exampleByKeySortingRankingTop3PriceProdInCat {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)


    val products = sc.
      textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/products_jl")

    val productsMap = products.
      map(rec => (rec.split(",")(1), rec))

    productsMap.
      groupByKey().
      flatMap(x => getTopDenseN(x, 2)).
      collect().
      foreach(el => println(s"Top 3 priced products in category: {el}"))


    def getTopDenseN(rec: (String, Iterable[String]), topN: Int): Iterable[String] = {
      var prodPrices: List[Float] = List()
      var topNPrices: List[Float] = List()
      var sortedRecs: List[String] = List()
      for(i <- rec._2) {
        prodPrices = prodPrices:+ i.split(",")(4).toFloat
      }
      topNPrices = prodPrices.distinct.sortBy(k => -k).take(topN)
      sortedRecs = rec._2.toList.sortBy(k => -k.split(",")(4).toFloat)
      var x: List[String] = List()
      for(i <- sortedRecs) {
        if(topNPrices.contains(i.split(",")(4).toFloat))
          x = x:+ i
      }
      return x
    }


  }
}
