package com.bbva.actor

import java.util.Properties

import akka.actor.Actor
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.slf4j.LoggerFactory
import com.bbva.service.{APICallBack, Message}

import scala.util.Random


class ProducerActorKafka extends Actor {

  val config = ConfigFactory.load()

  private def logger = LoggerFactory.getLogger(this.getClass)

  var producer: KafkaProducer[String, String] = makeKafkaProducer

  override def receive: Receive = {
    case Message(topic, id, message) =>
      producer.send(new ProducerRecord[String, String](topic, id, message),
        new APICallBack(System.currentTimeMillis(), topic, id, message))
    case "status" =>
      val partitions = producer.partitionsFor(config.getString("kafka.topic"))
      logger.info("request status: partitions " + partitions.size())
      sender() ! (partitions.size()>0)
  }

  def makeKafkaProducer: KafkaProducer[String, String] =
    new KafkaProducer[String, String](getConfiguration)


  def setKafkaProducer(kafkaProducer: KafkaProducer[String, String]){producer=kafkaProducer}

  private def getConfiguration: Properties = {

    val props: Properties = new Properties
    props.put("acks", config.getString("kafka.acks"))
    props.put("batch.size", config.getString("kafka.batch.size"))
    props.put("bootstrap.servers", config.getString("kafka.bootstrap.servers"))
    props.put("buffer.memory", config.getString("kafka.buffer.memory"))
    props.put("client.id", s"${config.getString("kafka.client.id")}-${Random.nextInt()}")
    props.put("connections.max.idle.ms", config.getString("kafka.connections.max.idle.ms"))
    props.put("key.serializer", config.getString("kafka.key.serializer"))
    props.put("linger.ms", config.getString("kafka.linger.ms"))
    props.put("max.in.flight.requests.per.connection",
      config.getString("kafka.max.in.flight.requests.per.connection"))
    props.put("partitioner.class", config.getString("kafka.partitioner.class"))
    props.put("retries", config.getString("kafka.retries"))
    props.put("request.timeout.ms", config.getString("kafka.request.timeout.ms"))
    props.put("value.serializer", config.getString("kafka.value.serializer"))

    logger.info(s"Connection created against " +
      s"bootstrap servers ${props.getProperty("bootstrap.servers")}.")

    props
  }
}
