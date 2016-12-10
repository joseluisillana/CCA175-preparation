from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-saveFileAsSequenceFileWithoutKey-py")
sc = SparkContext(conf=conf)

dataRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/departments_jl")

dataRDD.map(lambda x: (None,x)).saveAsSequenceFile("/user/joseluisillana1709/pruebas_spark/result/departmentsWithoutKeyAsSF")
