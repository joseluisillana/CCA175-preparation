import org.apache.spark.SparkContext, org.apache.spark.SparkConf
object SimpleApp {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("scala spark")
    val sc = new SparkContext(conf)
    val dataRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/departments_jl")
    dataRDD.saveAsTextFile("/user/joseluisillana1709/pruebas_spark/scalaspark/departmentsTesting")
  }
}
