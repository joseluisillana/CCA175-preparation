name := "SparkProjects"
version := "1.0"
scalaVersion := "2.10.4"
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.2.1",
  "org.apache.spark" % "spark-hive_2.10" % "1.2.1"
)