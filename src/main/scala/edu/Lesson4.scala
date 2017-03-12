package edu

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.concurrent.Await
import scala.concurrent.duration._

case class Lesson4(implicit val system: ActorSystem, materializer: ActorMaterializer) {

  import system.dispatcher

  //example of using materialized value which is a control value, rather than Future of result
  //Source.tick has materialized value of type Cancellable
  //we start another stream that returns Future[Done] to use it to signal the moment to cancel the first stream
  def example1() = {
    val cancellableTick = Source.tick(0.seconds, 500.millis, ()).toMat(Sink.foreach(println))(Keep.left)
    val cancelSignal = Source(1 to 5).throttle(1, 1.seconds, 1, ThrottleMode.shaping).toMat(Sink.ignore)(Keep.right)
    val cancellable = cancellableTick.run
    Await.result(cancelSignal.run.map(_ => cancellable.cancel()), Duration.Inf)
  }

  //counting words in a text file
  def example2() = {
    //I'm not using FileIO.fromPath, but scala.io.Source instead because of getLines method
    val fileSource = scala.io.Source.fromFile("input.txt")
    //flatMapConcat: for each element, transform it into a Source, then concatenate these Sources into one Source
    val wordsSource = Source.fromIterator(fileSource.getLines)
      .flatMapConcat(line => Source[String](line.split(Array(' ', '\n', '\t', '.')).toList))
      .filter(_.trim.length > 1)
    def countWord(map: Map[String, Int], word: String): Map[String, Int] =
      map.updated(word, map.getOrElse(word, 0) + 1)
    //folding wordcounts
    val stream =
      wordsSource.toMat(
        Sink.fold(Map[String, Int]())(countWord)
      )(Keep.right)
    val wordcounts = Await.result(stream.run, Duration.Inf)
    println(wordcounts.toSeq.sortBy(-_._2).take(100))
  }

  def call(example: Int) = example match {
    case 1 => example1()
    case 2 => example2()
//    case 3 => exercise3()
//    case 4 => example4()
//    case 5 => example5()
//    case 6 => example6()
//    case 7 => example7()
    case _ => println("wrong example")
  }
}

