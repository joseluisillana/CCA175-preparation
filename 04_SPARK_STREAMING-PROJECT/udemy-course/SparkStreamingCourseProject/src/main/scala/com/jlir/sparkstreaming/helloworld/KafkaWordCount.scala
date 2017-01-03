package com.jlir.sparkstreaming.helloworld

import java.io.FileNotFoundException

import com.fasterxml.jackson.databind.{JsonMappingException, JsonNode}
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.{JsonSchemaFactory, JsonValidator}
import org.apache.commons.io.IOUtils
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark._
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.json4s._
import org.json4s.jackson.JsonMethods._


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

    setupLogging

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


    // Separate the OK and KO events
    val (messageLines,messageLinesKO) = lines.map(record => (record.key, record.value)) doPartitionOnFilteringBy (value =>
      hasValidSchema(value._2.toString, schemaFile))


    // Total count By Window
    //val totalEventsByMinute = lines.countByWindow(Seconds(5),Seconds(1))
    //totalEventsByMinute.print()


    //(FR234567,bbva.es_front,/srv/mysrv,2007-11-03T13:18:05.423+01:00,2012,200)
    val mappedMessagesLines = messageLines.map(lineRaw => {
      implicit val formats = DefaultFormats
      ((parse(lineRaw._2) \ "self" \ "srvId").extract[String],
      (parse(lineRaw._2) \ "self" \ "srvType").extract[String],
      (parse(lineRaw._2) \ "self" \ "srvHref" ).extract[String],
      (parse(lineRaw._2) \ "self" \ "health" \ "timestamp").extract[String],
      (parse(lineRaw._2) \ "self" \ "health" \ "responseTime").extract[String],
      (parse(lineRaw._2) \ "self" \ "health" \ "httpStatus").extract[String])
    })

    val windowedMessageLines = mappedMessagesLines.window(Seconds(5),Seconds(1))


    windowedMessageLines.print()

    val avgOKResponseTime = windowedMessageLines.
      filter{
        case (srvId,srvType,srvHref,timestamp,responseTime,httpStatus) =>
          if (!httpStatus.isEmpty) {
            val respStatus = try { httpStatus.toInt }catch{ case e: Throwable => 500 }
            respStatus >= 200 && respStatus < 400
          }else{
            false
          }
        case _ =>
          false
      }.
      map(data => (data._2,data._5))
      //reduce((x:(String,String,Int),y:(String,String,Int)) => (x._1 + y._1, x._2+ y._2,x._3 + y._3))
      //reduce((x:(String,String,Int),y:(String,String,Int)) => x._3 + y._3)


   // avgOKResponseTime.print()

    val otro = avgOKResponseTime

    otro.print()

    // Need the avg of response time of the last 10 minutes on every minute
    //val avgResponseTimeByWindow = lines.countByWindow(Seconds(5),Seconds(1))

    //Count every 10 seconds for a type, avg time

    /*val words = messageLines.flatMapValues(_.split(" "))

    val wordCounts = words.map(x => (x, 1L))
      .reduceByKeyAndWindow(_ + _, _ - _, Minutes(10), Seconds(2), 2)

    wordCounts.print()*/

    ssc.start()
    ssc.awaitTermination()
  }

  def hasValidSchema(testJson: String, schemaFile: String): Boolean = {
    try
    {
      val schema: JsonNode = readResourceFileAsJSON(schemaFile)
      val instance: JsonNode = asJsonNode(parse(testJson))

      val validator : JsonValidator = JsonSchemaFactory.byDefault().getValidator

      val processingReport = validator.validate(schema, instance)

      // Gives more information about the possible errors on validations
      /*if (processingReport.isSuccess) {
        println("JSON Schema validation was successful")
      } else {
        processingReport.foreach { message: ProcessingMessage =>
          println(message.asJson())
        }
      }*/
      processingReport.isSuccess
    }
    catch
      {
        case jsonMappingException: JsonMappingException =>
          false

        case _ : Throwable =>
          false
      }


  }



  def readResourceFileAsJSON(fileName: String): JsonNode = {
    val inputFileStream = getClass.getResourceAsStream(s"/$fileName")
    val fileContent = IOUtils.toString(inputFileStream)
    val resultJson = Option(JsonLoader.fromString(fileContent))

    resultJson.getOrElse(throw new FileNotFoundException(fileName))
  }
  /** Makes sure only ERROR messages get logged to avoid log spam. */
  def setupLogging() = {
    import org.apache.log4j.{Level, Logger}
    val rootLogger = Logger.getRootLogger()
    rootLogger.setLevel(Level.ERROR)
  }

  implicit class DStreamsOperations[T](rdd: DStream[T]) {
    def doPartitionOnFilteringBy(f: T => Boolean): (DStream[T], DStream[T]) = {
      val passesFilter = rdd.filter(f)
      val failsOnFilter = rdd.filter(e => !f(e))
      (passesFilter, failsOnFilter)
    }


  }
}


