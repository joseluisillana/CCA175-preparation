from pyspark import SparkContext, SparkConf
from pyspark.sql import SQLContext, Row
conf = SparkConf().setAppName("pyspark-joiningOrderItemsWithNativeQuery-py")
sc = SparkContext(conf=conf)
sqlContext = SQLContext(sc)
sqlContext.sql("set spark.sql.shuffle.partitions=10");


ordersRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/orders_jl")
ordersMap = ordersRDD.map(lambda o: o.split(","))
orders = ordersMap.map(lambda o: Row(order_id=int(o[0]), order_date=o[1],
order_customer_id=int(o[2]), order_status=o[3]))
ordersSchema = sqlContext.inferSchema(orders)
ordersSchema.registerTempTable("orders")

orderItemsRDD = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/order_items_jl")
orderItemsMap = orderItemsRDD.map(lambda oi: oi.split(","))
orderItems = orderItemsMap.map(lambda oi: Row(order_item_id=int(oi[0]), order_item_order_id=int(oi[1]),
order_item_product_id=int(oi[2]), order_item_quantity=int(oi[3]), order_item_subtotal=float(oi[4]),
order_item_product_price=float(oi[5])))
orderItemsSchema = sqlContext.inferSchema(orderItems)
orderItemsSchema.registerTempTable("order_items")

joinAggData = sqlContext.sql("select o.order_date, sum(oi.order_item_subtotal), \
count(distinct o.order_id) from orders o join order_items oi \
on o.order_id = oi.order_item_order_id \
group by o.order_date order by o.order_date")

for data in joinAggData.collect():
  print(data)
