from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-joiningOrderItems-py")
sc = SparkContext(conf=conf)

ordersRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")
orderItemsRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/order_items_jl")

ordersParsedRDD = ordersRDD.map(lambda rec: (int(rec.split(",")[0]),rec))
orderItemsParsedRDD = orderItemsRDD.map(lambda rec: (int(rec.split(",")[1]),rec))

ordersJoinOrderItems = orderItemsParsedRDD.join(ordersParsedRDD)

revenuePerOrderPerDay = ordersJoinOrderItems.map(lambda t: (t[1][1].split(",")[1], float(t[1][0].split(",")[4])))

ordersPerDay = ordersJoinOrderItems.map(lambda rec: rec[1][1].split(",")[1] + "," + str(rec[0])).distinct()
ordersPerDayParsedRDD = ordersPerDay.map(lambda rec: (rec.split(",")[0], 1))
totalOrdersPerDay = ordersPerDayParsedRDD.reduceByKey(lambda x, y: x + y)

totalRevenuePerDay = revenuePerOrderPerDay.reduceByKey(lambda total1, total2: total1 + total2)

for data in totalRevenuePerDay.collect():
    print(data)

finalJoinRDD = totalOrdersPerDay.join(totalRevenuePerDay)

for data in finalJoinRDD.take(5):
    print(data)
