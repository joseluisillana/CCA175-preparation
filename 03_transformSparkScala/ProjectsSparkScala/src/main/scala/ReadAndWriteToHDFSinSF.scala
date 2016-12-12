import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

object ReadAndWriteToHDFSinSF {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("ReadAndWriteToHDFSinSF with spark and scala")
    val sc = new SparkContext(conf)
    val dataRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/departments_jl")

    val dataMap = dataRDD.map(x => (NullWritable,x))

    dataMap.saveAsSequenceFile("/user/joseluisillana1709/pruebas_spark/sparkresults/departmentsSequenceFile")
  }
}
