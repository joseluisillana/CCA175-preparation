from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-aggregatingOrderItems-py")
sc = SparkContext(conf=conf)

ordersRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")
ordersRDD.count()
