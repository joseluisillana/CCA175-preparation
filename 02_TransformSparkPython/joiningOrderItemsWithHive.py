from pyspark import SparkContext, SparkConf
from pyspark.sql import HiveContext
conf = SparkConf().setAppName("pyspark-joiningOrderItemsWithHive-py")
sc = SparkContext(conf=conf)
sqlContext = HiveContext(sc)
sqlContext.sql("set spark.sql.shuffle.partitions=10");


joinAggData = sqlContext.sql("select o.order_date, round(sum(oi.order_item_subtotal), 2), \
  count(distinct o.order_id) from retail_db_jlir.orders_jl o join retail_db_jlir.order_items_jl oi \
  on o.order_id = oi.order_item_order_id \
  group by o.order_date order by o.order_date")

for data in joinAggData.collect():
    print(data)
