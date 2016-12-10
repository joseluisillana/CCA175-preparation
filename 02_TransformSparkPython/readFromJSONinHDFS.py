from pyspark import SparkContext, SparkConf, SQLContext
conf = SparkConf().setAppName("pyspark-readFromJSONinHDFS-py")
sc = SparkContext(conf=conf)
sqlContext = SQLContext(sc)

departmentsJson = sqlContext.jsonFile("/user/joseluisillana1709/department_json/department.json")
departmentsJson.registerTempTable("departmentsTable")
departmentsData = sqlContext.sql("select * from departmentsTable")
for rec in departmentsData.collect():
    print(rec)

#Writing data in json format
departmentsData.toJSON().saveAsTextFile("/user/joseluisillana1709/pruebas_spark/result/departmentsJson")
