package com.sparkscala.aggtotals

import org.apache.spark.sql._
import org.apache.spark.{SparkConf, SparkContext}


object AggMaxProductPriceWithReduce {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName(s"${this.getClass.getName} with spark and scala")
    val sc = new SparkContext(conf)


    val productsRDD = sc.
      textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/products_jl")

    val productsMap = productsRDD.map(x => x)

    val result = productsMap.reduce((rec1, rec2) => (
      if(
        (if (rec1.split(",")(4) != "") rec1.split(",")(4).toFloat else 0 ) >=
          (if (rec2.split(",")(4) != "") rec2.split(",")(4).toFloat else 0))
        rec1
      else
        rec2)
    )

     println(s"Max price of product using reduce : ${result}"))
  }
}
