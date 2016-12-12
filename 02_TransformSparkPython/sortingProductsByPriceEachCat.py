from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-sortingProductsByPriceEachCat-py")
sc = SparkContext(conf=conf)

productsRAW = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/products_jl")


productsMap =  productsRAW.map(lambda rec: (int(rec.split(",")[1]), rec))

productsGroupBy = productsMap.groupByKey()


for item in productsGroupBy.take(100):
	print item

for i in productsGroupBy.map(lambda rec: sorted(rec[1], key=lambda k: float(k.split(",")[4] if (k.split(",")[4] != "" and not " " in k.split(",")[4]) else "12345.67"))).takeOrdered(5, lambda x[1]: -float(x[1].split(",")[4])):
	print(i)

for i in productsGroupBy.map(lambda rec: sorted(rec[1], key=lambda k: float(k.split(",")[4]), reverse=True)).take(100):
	print(i)
