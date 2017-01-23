package com.jlir.sparkstreaming.curro

import java.io.FileNotFoundException
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong

import com.fasterxml.jackson.databind.{JsonMappingException, JsonNode}
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.{JsonSchemaFactory, JsonValidator}
import org.apache.commons.io.IOUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.record.TimestampType
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringDeserializer, StringSerializer}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies._
import org.apache.spark.streaming.kafka010.LocationStrategies._
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, KafkaUtils, OffsetRange}
import org.apache.spark.streaming.{Seconds, StreamingContext, Time}
import org.apache.spark.util.LongAccumulator
import org.apache.spark.{SparkConf, SparkContext, TaskContext}
import org.joda.time.Duration
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._
/**
  * Created by joseluisillanaruiz on 9/1/17.
  */
object ReadKafkaSendKafka {

  def main(args: Array[String]) {
    if (args.length < 9) {
      System.err.println("Usage: KafkaReceiver <bootstrapServers URI in form host:port> <groupId> <list of topics, " +
        "comma separated> <num of threads> <batch interval> <window_interval> <slide_interval> <checkpoint-dir> <showtraces-flag>")
      System.exit(1)
    }
    // Gets the parameters
    val Array(bootstrapServers, group, topicsIn, topicsOut, numThreads, batchInterval, windowInterval, slideInterval, checkpointDir, showTraces) = args

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
    val Array(bootstrapServers, group, topicsIn, topicsOut, numThreads, batchInterval, windowInterval, slideInterval, checkpointDir, showTraces) = args

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
      "key.serializer" -> classOf[ByteArraySerializer].getName,
      "value.serializer" -> classOf[StringSerializer].getName,
      "group.id" -> (
        if (group == null || group.equals(""))
          appConfig.getString("kafka.group.id")
        else
          group
        ),
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )


    val kafkaProducer: Broadcast[MySparkKafkaProducer[Array[Byte], String]] = {
      ssc.sparkContext.broadcast(MySparkKafkaProducer[Array[Byte], String](kafkaParams))
    }

    // GETS topics and threads
    val topicInList = topicsIn.split(",").toSet
    val topicOut = topicsOut.split(",")(0)

    // GETS a DIRECTSTREAM
    val stream = KafkaUtils.createDirectStream(ssc, PreferConsistent,
      Subscribe[String, String](topicInList, kafkaParams))


    // reference to the most recently generated input rdd's offset ranges
    var offsetRanges = Array[OffsetRange]()

    val (streamMessagesOk, streamMessagesKo) = stream.transform { rdd =>
      // It's possible to get each input rdd's offset ranges, BUT...
      offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
      printOffsetsAndPartitionsBeforeWindow(offsetRanges, rdd, showTraces)
      rdd
    }.map(
      record => {
        mapDataFromConsumerRecord(record)
      }).
      doPartitionOnFilteringBy(value =>
        hasValidSchema(value._2._3.toString, schemaFile))


    if (showTraces.equals("true"))
    println(s"number of elements before windowing: ${streamMessagesOk.count()}")

    streamMessagesOk.window(Seconds(windowInterval.toInt), Seconds(slideInterval.toInt)).
      foreachRDD { (rdd, time) =>
        {
          processWindowedData(rdd, time, offsetRanges, showTraces, kafkaProducer, topicOut)
        }
      }

    // Set the checkpoint
    ssc.checkpoint((if (checkpointDir == null || checkpointDir.equals("")) appConfig.getString("application" +
      ".checkpointDir")
    else
      checkpointDir))
    ssc
  }

  def processWindowedData(rdd: RDD[(String, (String, Int, String, Long, Long, TimestampType, (String, String, String,
    String, String, String)))] , time: Time, offsetRanges: Array[OffsetRange], showTraces: String, kafkaProducer:
  Broadcast[MySparkKafkaProducer[Array[Byte], String]], topicOut: String ): Unit = {
    // As we could have multiple processes adding into these running totals
    // at the same time, we'll just Java's AtomicLong class to make sure
    // these counters are thread-safe.
    var accumEventsOk = new AtomicLong(0)
    var accumResponseTimeOk = new AtomicLong(0)
    var accumEventsKo = new AtomicLong(0)
    var accumResponseTimeKo = new AtomicLong(0)

    if (rdd.count() > 0) {

      rdd.collect().foreach(data => {
        accumResponseTimeOk.getAndAdd(data._2._4)

        (data._2._7._6, data._2._7._5) match {
          case (httpStatus: String, responseTime: String) =>
            if (!httpStatus.isEmpty && !responseTime.isEmpty) {
              val respStatus: Int = util.Try {
                httpStatus.toInt
              } getOrElse 500
              val respRTime: Long = util.Try {
                data._2._7._5.toLong
              } getOrElse 0

              if (respStatus >= 200 && respStatus < 400) {
                accumEventsOk.getAndAdd(1)
                accumResponseTimeOk.getAndAdd(respRTime)
              } else {
                accumEventsKo.getAndAdd(1)
                accumResponseTimeKo.getAndAdd(respRTime)
              }
            } else {
              accumEventsKo.getAndAdd(1)
              accumResponseTimeKo.getAndAdd(0)
            }
          case _ =>
            accumEventsKo.getAndAdd(1)
            accumResponseTimeKo.getAndAdd(0)
        }


      })

      if (showTraces.equals("true")) {
        //... if you then window, you're going to have partitions from multiple input rdds, not just the most recent one
        println(s"number of spark partitions after windowing: ${rdd.partitions.size}")
        println(s"number of elements after windowing: ${rdd.count()}")
      }
      val repartitionedRDD = rdd.repartition(1).cache()
      repartitionedRDD.foreachPartition { iter =>
        if (showTraces.equals("true")) {
          println("read offset ranges on the executor\n" + offsetRanges.mkString("\n"))
          // notice this partition ID can be higher than the number of partitions in a single input rdd
          println(s"this partition id ${TaskContext.get.partitionId}")

          iter.foreach(partitionItemData => {
            println(partitionItemData._2)
          })
        }
      }






    val avgResponseTimeOk: Long = util.Try(accumResponseTimeOk.get() / accumEventsOk
      .get()) getOrElse 0

    // Print totals from current window
    println("########################### Total success: " + Option(accumEventsOk.get()).getOrElse(0) + " Total failure: "
      + Option
    (accumEventsKo
      .get()).getOrElse(0))
    // Print AVG of response time from current window
    println("########################### OK AVG: " + Duration.standardSeconds(avgResponseTimeOk).toString + ".")

    // Don't alarm unless we have some minimum amount of data to work with
    if (accumEventsOk.get() + accumEventsKo.get() > 100) {
      // Compute the error rate
      // Note use of util.Try to handle potential divide by zero exception
      val ratio: Double = util.Try(accumEventsKo.doubleValue() / accumEventsOk.doubleValue()) getOrElse 1.0
      // If there are more errors than successes, wake someone up
      if (ratio > 0.5) {
        // In real life, you'd use JavaMail or Scala's courier library to send an
        // email that causes somebody's phone to make annoying noises, and you'd
        // make sure these alarms are only sent at most every half hour or something.
        println("########################### Wake somebody up! Something is horribly wrong.")
      } else {
        println("########################### All systems go.")
      }
    }
    val metadata: Stream[Future[RecordMetadata]] = Stream(kafkaProducer.value.sendKeyValueToTopic(
      topicOut,
      time.toString().getBytes,
      """{ "time": """ + time.toString() + """, """ + """"totalOk": """+ accumEventsOk.get() +
        """, "avgResponseTime": """ + avgResponseTimeOk.toDouble.toString + """}""".stripMargin))

    metadata.foreach { metadata =>
      println(metadata.get()) }

    } else {
      if (showTraces.equals("true"))
        println(s"NO NEW DATA: number of elements after windowing: ${rdd.count()}")
    }

  }

  def mapDataFromConsumerRecord(record: ConsumerRecord[String, String]): (String, (String, Int, String, Long, Long, TimestampType, (String, String, String, String, String, String))) = {
    implicit val formats = DefaultFormats
    (record.key(),
      (
        record.topic(),
        record.partition(),
        record.value(),
        record.offset(),
        record.timestamp(),
        record.timestampType(),
        (
          (parse(record.value()) \ "self" \ "srvId").extract[String],
          (parse(record.value()) \ "self" \ "srvType").extract[String],
          (parse(record.value()) \ "self" \ "srvHref").extract[String],
          (parse(record.value()) \ "self" \ "health" \ "timestamp").extract[String],
          (parse(record.value()) \ "self" \ "health" \ "responseTime").extract[String],
          (parse(record.value()) \ "self" \ "health" \ "httpStatus").extract[String]
          )
        )
      )
  }

  def printOffsetsAndPartitionsBeforeWindow(offsetRanges: Array[OffsetRange], rdd: RDD[ConsumerRecord[String,
    String]], showTraces: String): Unit = {
    if (showTraces.equals("true")) {
      println("got offset ranges on the driver:\n" + offsetRanges.mkString("\n"))
      println(s"number of kafka partitions before windowing: ${offsetRanges.size}")
      println(s"number of spark partitions before windowing: ${rdd.partitions.size}")
    }
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


  class MySparkKafkaProducer[K, V](createProducer: () => KafkaProducer[K, V]) extends Serializable {

    /* This is the key idea that allows us to work around running into
       NotSerializableExceptions. */
    lazy val producer = createProducer()

    def sendKeyValueToTopic(topic: String, key: K, value: V): Future[RecordMetadata] =
      producer.send(new ProducerRecord[K, V](topic, key, value))

    def sendValueToTopic(topic: String, value: V): Future[RecordMetadata] =
      producer.send(new ProducerRecord[K, V](topic, value))

  }

  object MySparkKafkaProducer {

    import scala.collection.JavaConversions._

    def apply[K, V](config: Map[String, Object]): MySparkKafkaProducer[K, V] = {
      val createProducerFunc = () => {
        val producer = new KafkaProducer[K, V](config)

        sys.addShutdownHook {
          // Ensure that, on executor JVM shutdown, the Kafka producer sends
          // any buffered messages to Kafka before shutting down.
          producer.close()
        }

        producer
      }
      new MySparkKafkaProducer(createProducerFunc)
    }
  }

}

