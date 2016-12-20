name := "spark-streaming-kafka"

version := "1.0"

scalaVersion := "2.10.6"

val overrideScalaVersion = "2.10.6"

// USING SPARK 2.0.0
//val sparkVersion = "2.0.0"
//val sparkXMLVersion = "0.3.3"
// val sparkCsvVersion = "1.4.0"
//val sparkElasticVersion = "2.3.4"
//val sscKafkaVersion = "1.6.2"
//val sparkMongoVersion = "1.0.0"
//val sparkCassandraVersion = "1.6.0"

//USING SPARK 1.1.1
val sparkVersion = "1.1.1"
val sscKafkaVersion = "1.6.2"

libraryDependencies ++= Seq(
  "org.apache.spark"      %%  "spark-core"      %   sparkVersion,
  "org.apache.spark"      %% "spark-sql"        % sparkVersion,
  "org.apache.spark"      %% "spark-streaming"  % sparkVersion,
  "org.apache.spark"      %% "spark-streaming-kafka"     % sscKafkaVersion
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}