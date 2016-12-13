import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}

object ReadFromHive {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName(s"${this.getClass.getName}  with spark and scala")
    val sc = new SparkContext(conf)
    val sqlContext = new HiveContext(sc)

    val departmentsRDD = sqlContext.sql("select * from retail_db_jlir.departments_jl")

    departmentsRDD.collect().foreach(println)
  }
}
