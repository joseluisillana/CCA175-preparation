import org.apache.hadoop.io._
import org.apache.spark.{SparkConf, SparkContext}

object ReadFromHDFSinSF {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName(s"${this.getClass.getName}  with spark and scala")
    val sc = new SparkContext(conf)

    val dataRDD = sc.sequenceFile("/user/joseluisillana1709/pruebas_spark/scalaspark/sparkresults/departmentsSequenceFileNewApi"
      ,classOf[IntWritable], classOf[Text])

    val dataMap = dataRDD.map(x => x.toString())

    dataMap.collect().foreach(println)
  }
}
