package com.helloworld

import java.io.FileNotFoundException

import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.{JsonSchemaFactory, JsonValidator}
import org.apache.commons.io.IOUtils
import org.apache.spark._
import org.apache.spark.streaming.kafka._
import org.apache.spark.streaming.{Minutes, Seconds, StreamingContext}

/**
  * Created by joseluisillana on 22/12/16.
  */
object KafkaWordCount {

  def main(args: Array[String]) {

    if (args.length < 4) {
      System.err.println("Usage: KafkaReceiver <zk URI in form host:port> <zk group> <list of topics, comma separated> <num of threads>")
      System.exit(1)
    }
    val appConfig = com.typesafe.config.ConfigFactory.load()

    val testJson = "{\"a\":\"b\"}"
    val schemaFile = "json-schema.json"
    //hasValidSchema(testJson, schemaFile)
    //check your report.

    // Create the context with a 1 second batch size
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("Kafka Work Count")
    val ssc = new StreamingContext(sparkConf, Seconds(1))

    // Gets the parameters
    val Array(zkHosts, group, topics, numThreads) = args

    // Set the checkpoint
    ssc.checkpoint("checkpoint")

    // GETS topics and threads
    val topicMap = topics.split(",").map((_, numThreads.toInt)).toMap

    // GETS a DSTREAM
    val lines = KafkaUtils.createStream(
      ssc, zkHosts, group, topicMap
    )


    val messageLines = lines.map(_._2).filter(value => hasValidSchema(value.toString, schemaFile).equals(true))

    val words = messageLines.flatMap(_.split(" "))

    val wordCounts = words.map(x => (x, 1L))
      .reduceByKeyAndWindow(_ + _, _ - _, Minutes(10), Seconds(2), 2)

    wordCounts.print()

    ssc.start()
    ssc.awaitTermination()
  }

  def hasValidSchema(testJson: String, schemaFile: String): Unit = {
    val factory: JsonSchemaFactory = JsonSchemaFactory.byDefault()
    val validator: JsonValidator = factory.getValidator
    val schemaJson: JsonNode = readResourceFileAsJSON(schemaFile)
    val report: ProcessingReport = validator.validate(schemaJson, JsonLoader.fromString(testJson))
  }

  def readResourceFileAsJSON(fileName: String): JsonNode = {
    val inputFileStream = getClass.getResourceAsStream(s"/$fileName")
    val fileContent = IOUtils.toString(inputFileStream)
    val resultJson = Option(JsonLoader.fromString(fileContent))

    resultJson.getOrElse(throw new FileNotFoundException(fileName))
  }
}

