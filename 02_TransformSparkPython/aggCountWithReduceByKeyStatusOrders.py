from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-aggCountWithReduceByKeyStatusOrders-py")
sc = SparkContext(conf=conf)

ordersRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")

ordersMap = ordersRDD.map(lambda x: (x.split(",")[3],1))

orderReduceByStatus = ordersMap.reduceByKey(lambda x, y: x + y)


print "###########################"
print "###########################"
for item in orderReduceByStatus.collect():
    print "USING REDUCE BY KEY: "+item
