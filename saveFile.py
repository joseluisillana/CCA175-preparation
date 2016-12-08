from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark")
sc = SparkContext(conf=conf)

dataRDD = sc.textFile("/user/cloudera/sqoop_import/departments")

for line in dataRDD.collect():
    print(line)

dataRDD.saveAsTextFile("/user/cloudera/pyspark/departmentsTesting")


