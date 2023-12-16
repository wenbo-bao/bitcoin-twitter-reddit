import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.Row
import java.util.Properties
import org.apache.spark.sql.functions._
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
//import org.apache.spark.sql.functions.udf
import org.apache.spark.sql.types.{DoubleType, StructField, StructType}
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

object TextSentiment {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder.appName("Text Sentiment").getOrCreate()
   

    val df = spark.read.option("header", "true")
      .option("multiLine", "true")
      .option("escape", "\"")
      .option("inferSchema", "true")
      .csv("bitcoin_reddit_clean.csv")
    //detectSentiment("This is an apple")
    val sentAnalysisUdf = udf(detectSentiment _)

    // error : 2011-5

    for (year <- 2010 to 2019) {
      val start = (year-1).toString() + "-12-1 00:00:00"
      val end = year.toString() + "-1-1 00:00:00"
      val newdf = df.filter(col("timestamp").between(start,end)) 
      val dfWithSentiment = newdf.withColumn("sentiment", sentAnalysisUdf(col("body"))).drop("body")
      val filepath = (year-1).toString()+"-12.csv"
      dfWithSentiment.write.option("header","true").mode("overwrite").csv(filepath)

      for (month <- 1 to 11) {
        val start = year.toString() + "-" + month.toString() + "-1 00:00:00"
        val end = year.toString() + "-" + (month+1).toString() + "-1 00:00:00"
        val newdf = df.filter(col("timestamp").between(start,end)) 
        val dfWithSentiment = newdf.withColumn("sentiment", sentAnalysisUdf(col("body"))).drop("body")
        val filepath = year.toString()+"-"+month.toString()+".csv"
        dfWithSentiment.write.option("header","true").mode("overwrite").csv(filepath)
      }
    }


      val start = "2019-12-1 00:00:00"
      val end = "2020-1-1 00:00:00"
      val newdf = df.filter(col("timestamp").between(start,end)) 
      val dfWithSentiment = newdf.withColumn("sentiment", sentAnalysisUdf(col("body"))).drop("body")
      val filepath = "2019-12.csv"
      dfWithSentiment.write.option("header","true").mode("overwrite").csv(filepath)


        
    
    //val dfWithSentiment = df.withColumn("sentiment", sentAnalysisUdf(col("body")))

    //val newdf = df.filter(col("timestamp").between("2018-8-1 00:00:00","2018-9-1 00:00:00"))
    //val result = resultDF.filter(col("timestamp").between("2016-1-1 00:00:00","2019-5-2 00:00:00")).distinct()
    
  //val rdd = newdf.rdd.map(row => {
    //val body = row.getAs[String]("body") + " "
    //val sentiment = detectSentiment(body)
    //Row.fromSeq(row.toSeq :+ sentiment) // Appends the result of sentA to each row
  //})

// Define the new schema with the additional column
//val newSchema = StructType(df.schema.fields :+ StructField("sentiment", DoubleType, nullable = true))

// Create a new DataFrame with the new schema
// val resultDF = spark.createDataFrame(rdd, newSchema).drop("body")
    //val resultDF = dfWithSentiment.drop("body")
    //resultDF.show()
    //Write to a new CSV
    //resultDF.write.option("header","true").mode("overwrite").csv("reddit_with_sentiment.csv")

    spark.stop()
  }

  val nlpProps = {
    val props = new Properties()
    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment")
    props
  }

  def detectSentiment(message: String): Double = {

    val pipeline = new StanfordCoreNLP(nlpProps)

    val annotation = pipeline.process(message)
    var sentiments: ListBuffer[Double] = ListBuffer()
    var sizes: ListBuffer[Int] = ListBuffer()

    var longest = 0
    var mainSentiment = 0

    for (sentence <- annotation.get(classOf[CoreAnnotations.SentencesAnnotation])) {
      val tree = sentence.get(classOf[SentimentCoreAnnotations.AnnotatedTree])
      val sentiment = RNNCoreAnnotations.getPredictedClass(tree)
      val partText = sentence.toString

      if (partText.length() > longest) {
        mainSentiment = sentiment
        longest = partText.length()
      }

      sentiments += sentiment.toDouble
      sizes += partText.length

      //println("debug: " + sentiment)
      //println("size: " + partText.length)

    }

    val averageSentiment:Double = {
      if(sentiments.size > 0) sentiments.sum / sentiments.size
      else -1
    }

    val weightedSentiments = (sentiments, sizes).zipped.map((sentiment, size) => sentiment * size)
    var weightedSentiment = weightedSentiments.sum / (sizes.fold(0)(_ + _))

    if(sentiments.size == 0) {
      mainSentiment = -1
      weightedSentiment = -1
    }


    //println("debug: main: " + mainSentiment)
    //println("debug: avg: " + averageSentiment)
    //println("debug: weighted: " + weightedSentiment)

    /*
     0 -> very negative
     1 -> negative
     2 -> neutral
     3 -> positive
     4 -> very positive
     */
    weightedSentiment match {
      case s if s <= 0.0 => -1.0
      case s if s < 1.0 => 0.0
      case s if s < 2.0 => 0.25
      case s if s < 3.0 => 0.5
      case s if s < 4.0 => 0.75
      case s if s < 5.0 => 1.0
      case s if s > 5.0 => -1.0
    }

  }

}

