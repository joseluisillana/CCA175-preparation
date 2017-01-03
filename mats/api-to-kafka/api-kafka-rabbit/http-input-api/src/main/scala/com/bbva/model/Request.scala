package com.bbva.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol


case class BulkLog(sourceSystem: String, message: String)

object BulkLog extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val responseJSON = jsonFormat2(BulkLog.apply)
}
