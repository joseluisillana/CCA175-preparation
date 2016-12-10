from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-wordCount-py")
sc = SparkContext(conf=conf)

datoRAW = sc.textFile("/user/joseluisillana1709/davincibook")
datoAplanado = datoRAW.flatMap(lambda x: x.split(" "))
datoConteo = datoAplanado.map(lambda word: (word, 1))

datoReducidoPorClave = datoConteo.reduceByKey(lambda x,y : x + y)

datoReducidoPorClave.saveAsTextFile("/user/joseluisillana1709/pruebas_spark/result/wordcountoutput")

for element in datoReducidoPorClave.collect():
    print(element)
