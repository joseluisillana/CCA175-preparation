from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-aggCountByStatusOrders-py")
sc = SparkContext(conf=conf)

ordersRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")

ordersMap = ordersRDD.map(lambda x: (x.split(",")[3],1))

ordersReduced = ordersMap.countByKey()

print "###########################"
print "###########################"
for item in ordersReduced.items():
    print "USING COUNT BY KEY: "+item
