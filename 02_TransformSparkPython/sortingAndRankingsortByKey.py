from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-sortingAndRankingsortByKey-py")
sc = SparkContext(conf=conf)

ordersRAW = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")
ordersMAP = ordersRAW.map(lambda rec: (int(rec.spli(",")[0]),rec))
ordersSorted = ordersMAP.sortByKey()

for item in ordersSorted.collect():
	print item


