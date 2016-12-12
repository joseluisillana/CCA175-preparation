from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-aggCountWithGroupByKeyStatusOrders-py")
sc = SparkContext(conf=conf)

ordersRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")

ordersMap = ordersRDD.map(lambda x: (x.split(",")[3],1))

orderByStatus = ordersMap.groupByKey().map(lambda x: (x[0],sum(x[1])))


print "###########################"
print "###########################"
for item in orderByStatus.collect():
    print "USING GROUP BY KEY: "+item
