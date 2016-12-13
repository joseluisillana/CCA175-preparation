import org.apache.spark.SparkContext._
import org.apache.spark.{SparkConf, SparkContext}

object ReadAndWriteToHDFSinSFKV {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName(s"${this.getClass.getName}  with spark and scala")
    val sc = new SparkContext(conf)

    val dataRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/departments_jl")

    val dataMap = dataRDD.map(x => (x.split(",")(0), x.split(",")(1)))

    dataMap.saveAsSequenceFile("/user/joseluisillana1709/pruebas_spark/scalaspark/sparkresults" +
      "/departmentsSequenceFile2")
  }
}
