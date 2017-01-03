package com.bbva.service

import akka.actor.ActorRef
import org.apache.kafka.clients.producer.{Callback, RecordMetadata}
import org.slf4j.LoggerFactory

class APICallBack(startTime: Long,
                  topic: String,
                  id: String,
                  message: String,
                  senderOpt: Option[ActorRef] = None) extends Callback{

  private def logger = LoggerFactory.getLogger(this.getClass)

  override def onCompletion(metadata: RecordMetadata, e: Exception): Unit = {
    if (metadata != null) {
      val elapsedTime = System.currentTimeMillis() - startTime
      logger.debug(s"message($topic,$message) " +
        s"with Id($id) sent to partition(${metadata.partition()})," +
        s"offset(${metadata.offset()}) in $elapsedTime ms")
      senderOpt.foreach(sender => sender ! true)
    } else {
      logger.error(e.getMessage)
      senderOpt.foreach(sender => sender ! false)
    }
  }

}
