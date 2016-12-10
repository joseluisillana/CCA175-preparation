from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-aggMaxPriceProductItems-py")
sc = SparkContext(conf=conf)


productsRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/products_jl")

productsMap = productsRDD.map(lambda rec: rec)

productsMap.reduce(
lambda rec1, rec2:
  (rec1
  if((rec1.split(",")[4] != "" and rec2.split(",")[4] != "")
  and float(rec1.split(",")[4]) >= float(rec2.split(",")[4]))
  else rec2)
)

for i in productsMap.take(5):
    print i
