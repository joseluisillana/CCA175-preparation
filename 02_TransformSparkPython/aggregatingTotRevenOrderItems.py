from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-aggregatingOrderItems-py")
sc = SparkContext(conf=conf)


orderItemsRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/order_items_jl")

orderItemsMap = orderItemsRDD.map(lambda rec: float(rec.split(",")[4]))
for i in orderItemsMap.take(5):
  print i

orderItemsReduce = orderItemsMap.reduce(lambda rev1, rev2: rev1 + rev2)


for i in orderItemsReduce.take(5):
    print i
