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
import org.apache.spark.util.LongAccumulator
import org.json4s._
import org.json4s.jackson.JsonMethods._


/**
  * Created by joseluisillana on 22/12/16.
  */
object KafkaWordCountDirect {

  def main(args: Array[String]) {
    if (args.length < 6) {
      System.err.println("Usage: KafkaReceiver <bootstrapServers URI in form host:port> <groupId> <list of topics, " +
        "comma separated> <num of threads> <batch interval> <checkpoint-dir>")
      System.exit(1)
    }
    // Gets the parameters
    val Array(bootstrapServers, group, topics, numThreads, batchInterval, checkpointDir) = args

    // Get old context or creates a new one
    val ssc = StreamingContext.getOrCreate(checkpointDir,() => functionToCreateContext(args))

    // Setup the log level
    setupLogging

    ssc.start()
    ssc.awaitTermination()
  }

  // Function to create and setup a new StreamingContext
  def functionToCreateContext(args: Array[String]): StreamingContext = {
    val appConfig = com.typesafe.config.ConfigFactory.load()

    val schemaFile = "json-schema.json"

    // Gets the parameters
    val Array(bootstrapServers, group, topics, numThreads, batchInterval, checkpointDir) = args

    // Create the context with a X seconds of batch interval
    val sparkConf = new SparkConf().setMaster(s"local[${numThreads}]").setAppName("Kafka Work Count")

    // Creates a new context
    val ssc = new StreamingContext(sparkConf, Seconds(batchInterval.toInt))

    // Setup the log level
    //setupLogging

    // Adjust the kafka parameters
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> (
        if (bootstrapServers == null || bootstrapServers.equals(""))
          appConfig.getString("kafka.bootstrap-servers")
        else
          bootstrapServers
        ),
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> (
        if (group == null || group.equals(""))
          appConfig.getString("kafka.group.id")
        else
          group
        ),
      "auto.offset.reset" -> appConfig.getString("kafka.auto.offset.reset"),
      "enable.auto.commit" -> (true: java.lang.Boolean)
    )

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

    val windowedMessageLines = mappedMessagesLines.window(Seconds(10),Seconds(4))

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


    avgOKResponseTime.foreachRDD((rdd,time) => {

      // Get or register the TotalEventsCounter Broadcast
      val totalEventsOnWindow = TotalEventsCounter.getInstance(rdd.sparkContext)

      //Don't want to deal with empty batches
      if(rdd.count() > 0){
        // Combine each partition's results into a single RDD:
        val repartitionedRDD = rdd.repartition(1).cache()

        // And print out a directory with the results.
        repartitionedRDD.saveAsTextFile("Events_" + time.milliseconds.toString)

        // Stop once we've collected 1000 tweets.
        totalEventsOnWindow.add(repartitionedRDD.count())

        println("totalEventsOnWindow count: " + totalEventsOnWindow.value)

        /*if (totalEventsOnWindow > 5) {
          System.exit(0)
        }*/
      }else{
        println("EL RRD TRAE 0 ¡¡¡¡ totalEventsOnWindow count: " + totalEventsOnWindow.value)
      }

    })

    // Set the checkpoint
    ssc.checkpoint((if (checkpointDir == null || checkpointDir.equals("")) appConfig.getString("application" +
      ".checkpointDir") else
      checkpointDir))
    ssc
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

  /**
    * Use this singleton to get or register an Accumulator.
    */
  object TotalEventsCounter {

    @volatile private var instance: LongAccumulator = null

    def getInstance(sc: SparkContext): LongAccumulator = {
      if (instance == null) {
        synchronized {
          if (instance == null) {
            instance = sc.longAccumulator("TotalEventsCounter")
          }
        }
      }
      instance
    }
  }

  implicit class DStreamsOperations[T](rdd: DStream[T]) {
    def doPartitionOnFilteringBy(f: T => Boolean): (DStream[T], DStream[T]) = {
      val passesFilter = rdd.filter(f)
      val failsOnFilter = rdd.filter(e => !f(e))
      (passesFilter, failsOnFilter)
    }


  }
}


