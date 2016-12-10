from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-readSequenceFileWithoutKey-py")
sc = SparkContext(conf=conf)

dataRDD = sc.sequenceFile("/user/joseluisillana1709/pruebas_spark/result/departmentsWithKeyAsSF","org.apache.hadoop.io.IntWritable","org.apache.hadoop.io.TextWritable")

for rec in dataRDD.collect():
    print(rec)
