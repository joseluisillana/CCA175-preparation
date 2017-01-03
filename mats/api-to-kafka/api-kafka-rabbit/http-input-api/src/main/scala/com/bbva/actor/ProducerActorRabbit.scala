package com.bbva.actor

import akka.actor.{Actor, ActorRef, ActorSystem}
import com.bbva.App
import com.bbva.service.{Message}
import com.rabbitmq.client.{AMQP, ConnectionFactory, ReturnListener}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

class ProducerActorRabbit(connFactory: ConnectionFactory, system: ActorSystem) extends Actor {

  val config = ConfigFactory.load()
  val routing = config.getString("rabbitmq.routingkey")
  private def logger = LoggerFactory.getLogger(this.getClass)

  def createRabbitProducer = {
    App.createConnectionFactoryRabbit.newConnection().createChannel()
  }

  var senderDataService:ActorRef = null
  val channel = createRabbitProducer

  override def receive: Receive = {
    /* Message to send a rabbit */
    case Message(topic, id, message) =>
      App.Stats.sumSend()
      channel.basicPublish(topic,
        routing,
        true,
        new AMQP.BasicProperties.Builder().build(),message.getBytes()
      )
    /* verify status rabbit connection. If response is false on healthCheck
    the api is disable */
    case "status" =>
      sender() ! channel.getConnection.isOpen

  }

  override def postStop() {
    logger.info("closing producer " + self)
    val connection = channel.getConnection
    channel.close()
    connection.close()
  }
}
