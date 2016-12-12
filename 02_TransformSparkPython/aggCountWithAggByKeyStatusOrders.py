from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-aggCountWithAggByKeyStatusOrders-py")
sc = SparkContext(conf=conf)

ordersRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")

ordersMap = ordersRDD.map(lambda x: (x.split(",")[3],x))

ordersByStatus = ordersMap.aggregateByKey(0,
  lambda accumulated, newvalue: accumulated+1,
  lambda accumulated, newvalue: accumulated+newvalue
)


print "###########################"
print "###########################"
for item in ordersByStatus.collect():
    print "USING AGGREGATE BY KEY: "+item
