package com.bbva

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.bbva.service.DataService
import com.rabbitmq.client.ConnectionFactory
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._


/**
  * @author ${user.name}
  */
object App extends scala.App {
  implicit val config = ConfigFactory.load()

  implicit val system = ActorSystem("rest")
  implicit val materializer = ActorMaterializer()
  implicit val timeout = Timeout(5 seconds)
  val actortype = config.getString("akka.actortype")
  val logger = Logging(system, getClass)

  //implicit val rabbitConnection = createConnectionFactoryRabbit

  /*Http().bindAndHandle(
    Route.handlerFlow(DataService(system, rabbitConnection).route),
    config.getString("http.interface"),
    config.getInt("http.port"))
*/
Http().bindAndHandle(
  Route.handlerFlow(DataService(system).route),
  config.getString("http.interface"),
  config.getInt("http.port"))


  /**
    * Method to create a connectionFactory of rabbit with specific
    * initialization parameters.
    * @return
    */
  def createConnectionFactoryRabbit: ConnectionFactory = {
    val connFactory = new ConnectionFactory()
    val user = config.getString("rabbitmq.user")
    val pass = config.getString("rabbitmq.pass")
    val rabbitserver = config.getString("rabbitmq.server")
    val rabbitport = config.getString("rabbitmq.server")
    connFactory.setUri(s"amqp://$user:$pass@$rabbitserver:$rabbitport")
    connFactory.setAutomaticRecoveryEnabled(true)
    connFactory.setConnectionTimeout(
      config.getInt("rabbitmq.connectionTimeOutValue"))
    connFactory.setNetworkRecoveryInterval(
      config.getInt("rabbitmq.networkRecoveryIntervalValue"))
    connFactory.setRequestedHeartbeat(
      config.getInt("rabbitmq.requestHeartBeatValue"))
    connFactory
  }

  /**
    * Object to obtain stats from the input api on first dates
    */
  object Stats{
    // Number request messages
    var contsend:Long = 0
    // Number send messages to rabbit
    var contrequest:Long = 0

    def sumRequest(): Unit = {
      this.synchronized {
        contrequest = contrequest + 1
      }
    }


    def sumSend(): Unit = {
      this.synchronized {
        contsend = contsend + 1
      }
    }

    def paintCount(): Unit ={
      logger.info(s"Number request messages: $contrequest")
      logger.info(s"Number send messages to rabbit: $contsend")
    }

  }
}
