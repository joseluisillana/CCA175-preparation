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
object KafkaWindowedDirect {

  def main(args: Array[String]) {
    if (args.length < 6) {
      System.err.println("Usage: KafkaReceiver <bootstrapServers URI in form host:port> <groupId> <list of topics, " +
        "comma separated> <num of threads> <batch interval> <checkpoint-dir>")
      System.exit(1)
    }
    // Gets the parameters
    val Array(bootstrapServers, group, topics, numThreads, batchInterval, checkpointDir) = args

    // Get old context or creates a new one
    val ssc = StreamingContext.getOrCreate(checkpointDir, () => functionToCreateContext(args))

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
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    // GETS topics and threads
    val topicList = topics.split(",").toSet

    // GETS a DIRECTSTREAM
    val stream = KafkaUtils.createDirectStream(ssc, PreferConsistent,
      Subscribe[String, String](topicList, kafkaParams))


    // reference to the most recently generated input rdd's offset ranges
    var offsetRanges = Array[OffsetRange]()

    stream.transform { rdd =>
      // It's possible to get each input rdd's offset ranges, BUT...
      offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
      println("got offset ranges on the driver:\n" + offsetRanges.mkString("\n"))
      println(s"number of kafka partitions before windowing: ${offsetRanges.size}")
      println(s"number of spark partitions before windowing: ${rdd.partitions.size}")
      rdd
    }.map(
      record => (record.key(), (record.topic(), record.partition(), record.value(), record.offset(),
        record.timestamp(), record.timestampType()))
    ).window(Seconds(6),Seconds(2))
      .foreachRDD { rdd =>
        //... if you then window, you're going to have partitions from multiple input rdds, not just the most recent one
        println(s"number of spark partitions after windowing: ${rdd.partitions.size}")
        val repartitionedRDD = rdd.repartition(1).cache()
        repartitionedRDD.foreachPartition { iter =>
          println("read offset ranges on the executor\n" + offsetRanges.mkString("\n"))
          // notice this partition ID can be higher than the number of partitions in a single input rdd
          println(s"this partition id ${TaskContext.get.partitionId}")
          iter.foreach(partitionItemData => {
            println(partitionItemData._2)
          })

        }
        // Moral of the story:
        // If you just care about the most recent rdd's offset ranges, a single reference is fine.
        // If you want to do something with all of the offset ranges in the window,
        // you need to stick them in a data structure, e.g. a bounded queue.

        // But be aware, regardless of whether you use the createStream or createDirectStream api,
        // you will get a fundamentally wrong answer if your job fails and restarts at something other than the highest offset,
        // because the first window after restart will include all messages received while your job was down,
        // not just X seconds worth of messages.

        // In order to really solve this, you'd have to time-index kafka,
        // and override the behavior of the dstream's compute() method to only return messages for the correct time.
        // Or do your own bucketing into a data store based on the time in the message, not system clock at time of reading.

        // Or... don't worry about it :)
        // Restart the stream however you normally would (checkpoint, or save most recent offsets, or auto.offset.reset, whatever)
        // and accept that your first window will be wrong


      }



    // Set the checkpoint
    ssc.checkpoint((if (checkpointDir == null || checkpointDir.equals("")) appConfig.getString("application" +
      ".checkpointDir")
    else
      checkpointDir))
    ssc
  }

  def hasValidSchema(testJson: String, schemaFile: String): Boolean = {
    try {
      val schema: JsonNode = readResourceFileAsJSON(schemaFile)
      val instance: JsonNode = asJsonNode(parse(testJson))

      val validator: JsonValidator = JsonSchemaFactory.byDefault().getValidator

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
    catch {
      case jsonMappingException: JsonMappingException =>
        false
      case _: Throwable =>
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


