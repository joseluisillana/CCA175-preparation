from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-saveFile-py")
sc = SparkContext(conf=conf)

dataRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/departments_jl")

for line in dataRDD.collect():
    print(line)

dataRDD.saveAsTextFile("/user/joseluisillana1709/pruebas_spark/result/departmentsTesting")
