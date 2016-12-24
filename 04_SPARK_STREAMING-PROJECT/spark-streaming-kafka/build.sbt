name := "spark-streaming-kafka"
organization := "com.jlir.sparkstreaming"
version := "1.0"

scalaVersion := "2.10.6"

val overrideScalaVersion = "2.10.6"

//USING SPARK 1.1.1
val sparkVersion = "1.6.3"
val sscKafkaVersion = "1.6.3"
val jackson = "2.8.5"

libraryDependencies ++= Seq(
  "org.apache.spark"      %%  "spark-core"      %   sparkVersion,
  "org.apache.spark"      %% "spark-sql"        % sparkVersion,
  "org.apache.spark"      %% "spark-streaming"  % sparkVersion,
  "org.apache.spark"      %% "spark-streaming-kafka"     % sscKafkaVersion,
  "com.fasterxml.jackson.core" % "jackson-core" % jackson,
"com.github.fge" % "json-schema-validator" % "2.2.6"
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}