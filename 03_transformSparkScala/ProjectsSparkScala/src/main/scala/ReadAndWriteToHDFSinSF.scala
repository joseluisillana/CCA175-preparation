import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.hadoop.io._
import org.apache.hadoop.mapreduce.lib.output._

object ReadAndWriteToHDFSinSF {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("ReadAndWriteToHDFSinSF with spark and scala")
    val sc = new SparkContext(conf)
    val sqlContext= new org.apache.spark.sql.SQLContext(sc)
    import sqlContext.implicits._
    val dataRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/departments_jl")

    val dataMap = dataRDD.map(x => (NullWritable.get(),x))

    dataMap.saveAsSequenceFile("/user/joseluisillana1709/pruebas_spark/sparkresults/departmentsSequenceFile")
  }
}
