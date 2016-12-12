from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-sortingAWithTakeOrdered-py")
sc = SparkContext(conf=conf)

ordersRAW = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")


for i in ordersRAW.map(lambda rec: (int(rec.split(",")[0]), rec)).takeOrdered(5, lambda x: x[0]):
	print(i)

for i in ordersRAW.map(lambda rec: (int(rec.split(",")[0]), rec)).takeOrdered(5, lambda x: -x[0]):
	print(i)

for i in ordersRAW.takeOrdered(5, lambda x: int(x.split(",")[0])):
	print(i)

for i in ordersRAW.takeOrdered(5, lambda x: -int(x.split(",")[0])):
	print(i)
