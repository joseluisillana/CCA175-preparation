package com.jlir.sparkstreaming.udemycourse

import java.util.regex.Matcher

import com.jlir.sparkstreaming.udemycourse.Utilities._
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming._

/** An example of using a State object to keep persistent state information across a stream.
  *  In this case, we'll keep track of clickstreams on sessions tied together by IP addresses.
  */
object Sessionizer {

  /** This "case class" lets us quickly define a complex type that contains a session length and a list of URL's visited,
    *  which makes up the session data state that we want to preserve across a given session. The "case class" automatically
    *  creates the constructor and accessors we want to use.
    */
  case class SessionData(val sessionLength: Long, val reqOK: Long, val reqKO: Long, var clickstream:List[String]);

  /** This function gets called as new data is streamed in, and maintains state across whatever key you define. In this case,
    *  it expects to get an IP address as the key, a String as a URL (wrapped in an Option to handle exceptions), and
    *  maintains state defined by our SessionData class defined above. Its output is new key, value pairs of IP address
    *  and the updated SessionData that takes this new line into account.
    */
  def trackStateFunc(batchTime: Time, ip: String, data: Option[(String, Boolean)], state: State[SessionData]): Option[(String, SessionData)] = {

    // Extract the previous state passed in (using getOrElse to handle exceptions)
    val previousState = state.getOption.getOrElse(SessionData(0, 0, 0, List()))

    val url: Option[String] = Some(data.get._1)
    val reqOk: Boolean = data.get._2

    var numReqsOK: Long = previousState.reqOK
    var numReqsKO: Long = previousState.reqKO

    reqOk match {
      case true =>
        numReqsOK += 1L
      case false =>
        numReqsKO += 1L
      case _ =>
        numReqsKO += 1L
    }
    // Create a new state that increments the session length by one, adds this URL to the clickstream, and clamps the clickstream
    // list to 10 items
    val newState = SessionData(previousState.sessionLength + 1L,
      numReqsOK,
      numReqsKO,
      (previousState.clickstream :+ url.getOrElse("empty")).take(10))

    // Update our state with the new state.
    state.update(newState)

    // Return a new key/value result.
    Some((ip, newState))
  }

  // Needed only on Windows:
  //System.setProperty("spark.sql.warehouse.dir","file:///C:/temp")

  def main(args: Array[String]) {

    // Create the context with a 1 second batch size
    val ssc = new StreamingContext("local[*]", "Sessionizer", Seconds(1))

    setupLogging()

    // Construct a regular expression (regex) to extract fields from raw Apache log lines
    val pattern = apacheLogPattern()

    // We'll define our state using our trackStateFunc function above, and also specify a
    // session timeout value of 30 minutes.
    val stateSpec = StateSpec.function(trackStateFunc _).timeout(Minutes(30))

    // Create a socket stream to read log data published via netcat on port 9999 locally
    val lines = ssc.socketTextStream("127.0.0.1", 9999, StorageLevel.MEMORY_AND_DISK_SER)

    // Extract the (ip, url) we want from each log line
    val requests = lines.map(x => {
      val matcher:Matcher = pattern.matcher(x)
      if (matcher.matches()) {
        val ip = matcher.group(1)
        val request = matcher.group(5)
        val requestFields = request.toString().split(" ")
        val url = scala.util.Try(requestFields(1)) getOrElse "[error]"
        (ip, (url, true))
      } else {
        ("error", ("error", false))
      }
    })

    // Now we will process this data through our StateSpec to update the stateful session data
    // Note that our incoming RDD contains key/value pairs of ip/URL, and that what our
    // trackStateFunc above expects as input.
    val requestsWithState = requests.mapWithState(stateSpec)

    // And we'll take a snapshot of the current state so we can look at it.
    val stateSnapshotStream = requestsWithState.stateSnapshots()

    // Process each RDD from each batch as it comes in
    stateSnapshotStream.foreachRDD((rdd, time) => {

      // We'll expose the state data as SparkSQL, but you could update some external DB
      // in the real world.

      // Get the singleton instance of SQLContext
      val sqlContext = SQLContextSingleton.getInstance(rdd.context)
      import sqlContext.implicits._

      // Slightly different syntax here from our earlier SparkSQL example. toDF can take a list
      // of column names, and if the number of columns matches what's in your RDD, it just works
      // without having to use an intermediate case class to define your records.
      // Our RDD contains key/value pairs of IP address to SessionData objects (the output from
      // trackStateFunc), so we first split it into 3 columns using map().
      val requestsDataFrame = rdd.map(x => (x._1, x._2.sessionLength, x._2.reqOK, x._2.reqKO, x._2.clickstream)).toDF
      ("ip", "sessionLength", "reqOK", "reqKO", "clickstream")

      // Create a SQL table from this DataFrame
      requestsDataFrame.createOrReplaceTempView("sessionData")

      // Dump out the results - you can do any SQL you want here.
      val sessionsDataFrame =
        sqlContext.sql("select * from sessionData")
      println(s"========= $time =========")
      sessionsDataFrame.show()

    })

    // Kick it off
    ssc.checkpoint("checkpointSessionizer")
    ssc.start()
    ssc.awaitTermination()
  }
}

/** Lazily instantiated singleton instance of SQLContext
  *  (Straight from included examples in Spark)  */
/*object SQLContextSingleton {

  @transient  private var instance: SQLContext = _

  def getInstance(sparkContext: SparkContext): SQLContext = {
    if (instance == null) {
      instance = new SQLContext(sparkContext)
    }
    instance
  }
}*/