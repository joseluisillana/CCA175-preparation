name := "spark-streaming-course-project"
organization := "com.jlir.sparkstreaming"
version := "1.0"

scalaVersion := "2.11.8"

val overrideScalaVersion = "2.11.8"

//USING SPARK 1.1.1
val sparkVersion = "2.0.2"
val sscKafkaVersion = "2.0.2"
val sscTwitterVersion = "1.6.0"
val jackson = "2.8.5"
val jacksonSchemaValidator = "2.2.6"
val typesafeConfigVersion = "1.3.1"
val twitterVersion = "4.0.6"

libraryDependencies ++= Seq(
  "org.apache.spark"      %%  "spark-core"      %   sparkVersion,
  "org.apache.spark"      %% "spark-sql"        % sparkVersion,
  "org.apache.spark"      %% "spark-streaming"  % sparkVersion,
  "org.apache.spark"      %% "spark-streaming-kafka-0-10" % sscKafkaVersion,
  "com.fasterxml.jackson.core" % "jackson-core" % jackson,
  "com.github.fge" % "json-schema-validator" % "2.2.6",
  "com.typesafe" % "config" % "1.3.1",
  "org.twitter4j" % "twitter4j-core" % twitterVersion,
  "org.twitter4j" % "twitter4j-stream" % twitterVersion,
  "org.apache.spark" %% "spark-streaming-twitter" % sscTwitterVersion
  //"org.spark-project" %% "dstream-twitter" % "0.1.0"

)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
