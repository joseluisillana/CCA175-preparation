package com.jlir.sparkstreaming.helloworld

import java.io.FileNotFoundException

import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.{JsonSchemaFactory, JsonValidator}
import org.apache.commons.io.IOUtils
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Minutes, Seconds, StreamingContext}

/**
  * Created by joseluisillana on 22/12/16.
  */
object KafkaWordCount {

  def main(args: Array[String]) {

    if (args.length < 6) {
      System.err.println("Usage: KafkaReceiver <bootstrapServers URI in form host:port> <groupId> <list of topics, " +
        "comma separated> <num of threads> <batch interval> <checkpoint-dir>")
      System.exit(1)
    }
    val appConfig = com.typesafe.config.ConfigFactory.load()

    val testJson = "{\"a\":\"b\"}"
    val schemaFile = "json-schema.json"
    //hasValidSchema(testJson, schemaFile)
    //check your report.

    // Gets the parameters
    val Array(bootstrapServers, group, topics, numThreads, batchInterval, checkpointDir) = args
    // Create the context with a 1 second batch size

    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("Kafka Work Count")
    val ssc = new StreamingContext(sparkConf, Seconds(batchInterval.toInt))


    // Adjust the kafka parameters
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> (if (bootstrapServers == null || bootstrapServers.equals("")) appConfig.getString("kafka" +
      ".bootstrap-servers") else bootstrapServers),
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> (if (group == null || group.equals("")) appConfig.getString("kafka.group.id") else group),
      "auto.offset.reset" -> appConfig.getString("kafka.auto.offset.reset"),
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    // Set the checkpoint
    ssc.checkpoint((if (checkpointDir == null || checkpointDir.equals("")) appConfig.getString("application.checkpointDir") else
      checkpointDir))

    // GETS topics and threads
    val topicList = topics.split(",")

    // GETS a DIRECTSTREAM
    val lines = KafkaUtils.createDirectStream(ssc,PreferConsistent,
      Subscribe[String, String](topicList, kafkaParams))

    val messageLines = lines.map(record => (record.key, record.value()))
      //.filter(value => hasValidSchema(value.toString,
      //schemaFile).equals(true))

    val words = messageLines.flatMapValues(_.split(" "))

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

