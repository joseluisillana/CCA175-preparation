from pyspark.sql import HiveContext
from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-readSFromAHiveTable-py")
sc = SparkContext(conf=conf)
sqlContext = HiveContext(sc)

dataRDD = sqlContext.sql("SELECT * FROM retail_db_jlir.departments_jl;").collect()

for row in dataRDD:
    print(row)
