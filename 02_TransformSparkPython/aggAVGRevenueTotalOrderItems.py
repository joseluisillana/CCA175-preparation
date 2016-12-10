from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-aggAVGRevenueTotalOrderItems-py")
sc = SparkContext(conf=conf)

revenue = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/order_items_jl").
map(lambda rec: float(rec.split(",")[4])).
reduce(lambda rev1, rev2: rev1 + rev2)

totalOrders = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/order_items_jl").
map(lambda rec: int(rec.split(",")[1])).
distinct().
count()

result = revenue/totalOrders

print result
