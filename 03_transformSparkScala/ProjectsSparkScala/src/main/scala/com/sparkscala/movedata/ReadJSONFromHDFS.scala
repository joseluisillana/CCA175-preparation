package com.sparkscala.movedata

import org.apache.spark.sql._
import org.apache.spark.{SparkConf, SparkContext}

object ReadJSONFromHDFS {
  def main(args: Array[String]) {
    val conf = new SparkConf()
      .setAppName(s"${this.getClass.getName}  with spark and scala")

    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)

    val dataRDD = sqlContext.jsonFile("/user/joseluisillana1709/pruebas_spark/raw/department_json" +
      "/department.json")

    dataRDD.registerTempTable("departmentsTable")

    val departmentsData = sqlContext.
      sql("select * from departmentsTable")

    departmentsData.collect().foreach(println)

    //Writing data in json format
    departmentsData.toJSON.saveAsTextFile("/user/joseluisillana1709/pruebas_spark/result/departmentsScalaJson")

  }
}
