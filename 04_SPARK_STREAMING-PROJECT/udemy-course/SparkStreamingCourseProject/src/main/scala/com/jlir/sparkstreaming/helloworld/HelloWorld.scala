package com.jlir.sparkstreaming.helloworld

/**
  * Created by joseluisillana on 20/12/16.
  */
object HelloWorld {
  def main(args: Array[String]): Unit = {
    val conf = com.typesafe.config.ConfigFactory.load()
    val label = conf.getString("aws.kafka-bootstrap")

    println(s"Hello World! ${label}")
  }
}
