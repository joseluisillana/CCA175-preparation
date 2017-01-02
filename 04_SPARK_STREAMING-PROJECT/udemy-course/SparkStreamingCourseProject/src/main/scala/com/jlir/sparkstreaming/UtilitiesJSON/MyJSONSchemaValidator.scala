package com.jlir.sparkstreaming.UtilitiesJSON

import scala.collection.JavaConversions._
import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jsonschema.core.report.ProcessingMessage
import com.github.fge.jsonschema.main.JsonSchemaFactory
import org.json4s._
import org.json4s.jackson.JsonMethods._


/**
  * Created by joseluisillana on 2/01/17.
  */
object MyJSONSchemaValidator {

  val json = """{"hello":"world"}"""
  val jsonSchema = """{
                     |  "title":"hello world schema",
                     |  "type":"object",
                     |  "properties":{
                     |    "hello": {
                     |      "type": "string"
                     |    }
                     |  },
                     |  "required":["hello"]
                     |}""".stripMargin

  val schema: JsonNode = asJsonNode(parse(jsonSchema))
  val instance: JsonNode = asJsonNode(parse(json))

  val validator = JsonSchemaFactory.byDefault().getValidator

  val processingReport = validator.validate(schema, instance)

  if (processingReport.isSuccess) {
    println("JSON Schema validation was successful")
  } else {
    processingReport.foreach { message: ProcessingMessage =>
      println(message.asJson())
    }
  }
}