import java.util.concurrent.Executors

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object Immutability {
  val aantalThreads = 8
  val aantalKeerOptellen = 100000

  implicit val context: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(aantalThreads))

  def main(args: Array[String]): Unit = {
    val mutableCounter = new MutableCounter(0)
    val immutableCounter = new ImmutableCounter(0)

    repeatMultiThreaded(aantalKeerOptellen, () => mutableCounter.add(1))
    val additions = repeatMultiThreaded(aantalKeerOptellen, () => ImmutableCounter.add(1))

    val newImmutableCounter = applyAll(additions)(immutableCounter)

    println(s"Na $aantalKeerOptellen keer tellen staat de MutableCounter op ${mutableCounter.count}")
    println(s"Na $aantalKeerOptellen keer tellen staat de ImmutableCounter op ${newImmutableCounter.count}")

    System.exit(0)
  }

  def repeatMultiThreaded(times: Int, action: () => Unit): Unit = {
    val futures = for (_ <- 0 until times) yield Future {
      action()
    }
    Await.ready(Future.sequence(futures), Duration.Inf)
  }

  def repeatMultiThreaded[T](times: Int, action: () => T): Seq[T] = {
    val futures = for (_ <- 0 until times) yield Future {
      action()
    }
    Await.result(Future.sequence(futures), Duration.Inf)
  }

  private def applyAll[T](functions: Seq[T => T])(t: T): T =
    if (functions.isEmpty) t
    else applyAll(functions.tail)(functions.head(t))
}

class MutableCounter(var count: Int) {
  def add(amount: Int): Unit =
    count = count + amount
}

class ImmutableCounter(val count: Int)

object ImmutableCounter {
  def add(amount: Int): ImmutableCounter => ImmutableCounter =
    counter => new ImmutableCounter(counter.count + amount)
}