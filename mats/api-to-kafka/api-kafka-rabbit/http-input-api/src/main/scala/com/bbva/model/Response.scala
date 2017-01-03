package com.bbva.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, DeserializationException, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

case class IdElement(id: String)

object IdElement extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val responseJSON = jsonFormat1(IdElement.apply)
}

case class LogResponse(data: IdElement)

object LogResponse extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val responseJSON = jsonFormat1(LogResponse.apply)
}

case class BulkResponse(data: List[IdElement])

object BulkResponse extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val responseJSON = jsonFormat1(BulkResponse.apply)
}

case class HealthCheckResponse(path: String, apiStatus: String, rabbitStatus: String,
                               numRequests: Long, numSend: Long
                               )

object HealthCheckResponse extends DefaultJsonProtocol with SprayJsonSupport {

  // implicit val responseJSON = jsonFormat7(HealthCheckResponse.apply)
  implicit object HealthCheckResponseFormat extends RootJsonFormat[HealthCheckResponse] {
    def write(healthData: HealthCheckResponse) = JsObject(
      // "path" -> JsString(healthData.path),
      "apiStatus" -> JsString(healthData.apiStatus),
      "status" -> JsString(healthData.rabbitStatus),
      "numRequests" -> JsString(healthData.numRequests.toString),
      "numSend" -> JsString(healthData.numSend.toString)
    )


    def read(value: JsValue): HealthCheckResponse = {
      value.asJsObject.getFields("path", "apiStatus", "rabbitStatus", "numRequests",
        "numSend") match {
        case Seq(JsString(path),
        JsString(apiStatus),
        JsString(rabbitStatus),
        JsNumber(numRequests),
        JsNumber(numSend)) =>
          new HealthCheckResponse(
            path,
            apiStatus,
            rabbitStatus,
            numRequests.toInt,
            numSend.toInt
            )
        case _ => throw new DeserializationException("HealthCheckResponse expected")
      }
    }
  }

}

case class HealthCheckDataResponse(data: HealthCheckResponse)

object HealthCheckDataResponse extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val responseJSON = jsonFormat1(HealthCheckDataResponse.apply)
}
