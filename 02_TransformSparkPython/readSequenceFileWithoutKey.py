from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-readSequenceFileWithoutKey-py")
sc = SparkContext(conf=conf)

dataRDD = sc.sequenceFile("/user/joseluisillana1709/pruebas_spark/result/departmentsWithoutKeyAsSF")

for rec in dataRDD:
    print(rec)
