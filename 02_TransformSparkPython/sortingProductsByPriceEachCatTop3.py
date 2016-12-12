from pyspark import SparkContext, SparkConf
conf = SparkConf().setAppName("pyspark-sortingProductsByPriceEachCatTop3-py")
sc = SparkContext(conf=conf)

productsRAW = sc.textFile("/user/joseluisillana1709/pruebas_spark/raw/sqoop_import/products_jl")


productsMap =  productsRAW.map(lambda rec: (int(rec.split(",")[1]), rec))

productsGroupBy = productsMap.groupByKey()

#for i in productsGroupBy.flatMap(lambda x: getTopDenseN(x, 2)).collect():
#	print(i)

def getTopDenseN(rec, topN):
    x = [ ]
    topNPrices = [ ]
    prodPrices = [ ]
    prodPricesDesc = [ ]
    for i in rec[1]:
        prodPrices.append(float(i.split(",")[4] if (i.split(",")[4] != "" and not " " in i.split(",")[4]) else "12345.67"))
        prodPricesDesc = list(sorted(set(prodPrices), reverse=True))
    import itertools
    topNPrices = list(itertools.islice(prodPricesDesc, 0, topN))
    for j in sorted(rec[1], key=lambda k: float(k.split(",")[4] if (k.split(",")[4] != "" and not " " in k.split(",")[4]) else "12345.67"), reverse=True):
        if(float(j.split(",")[4] if (j.split(",")[4] != "" and not " " in j.split(",")[4]) else "12345.67") in topNPrices):
            x.append(j)
    return (y for y in x)

for i in productsGroupBy.flatMap(lambda x: getTopDenseN(x, 2)).collect():
    print(i)
