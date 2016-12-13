name := "SparkProjects"
version := "1.0"
scalaVersion := "2.10.4"
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.5.2",
  "org.apache.spark" % "spark-hive_2.10" % "1.2.1"
)