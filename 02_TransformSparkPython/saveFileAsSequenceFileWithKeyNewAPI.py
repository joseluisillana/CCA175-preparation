from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-saveFileAsSequenceFileWithKey-py")
sc = SparkContext(conf=conf)

dataRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/departments_jl")

dataRDD.map(lambda x: tuple(x.split(",",1))).saveAsNewAPIHadoopFile("/user/joseluisillana1709/pruebas_spark/result/departmentsWithKeyAsSFNewAPI","org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat",keyClass="org.apache.hadoop.io.Text",valueClass="org.apache.hadoop.io.Text")
