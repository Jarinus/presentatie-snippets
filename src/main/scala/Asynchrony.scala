import java.util.concurrent.Executors

import Asynchrony._
import AsynchronyHelper._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object Asynchrony {
  val aantalStukkenHout = 4
  val aantalThreads = 2
  val zaagTijdMs = 1000

  def main(args: Array[String]): Unit = {
    val logs = List.range(0, aantalStukkenHout).map(i => new Log(i))

    println("Synchroon:")
    val synchronousDuration = executionTime(() => SynchronousSawmill.saw(logs))
    println(s"Synchroon houtzagen duurde ${synchronousDuration}s.")

    println()
    println("Asynchroon:")
    val asynchronousDuration = executionTime(() => AsynchronousSawmill.saw(logs))
    println(s"Asynchroon houtzagen duurde ${asynchronousDuration}s.")

    println()
    println(
      "Asynchroon houtzagen is "
        + divide(synchronousDuration, asynchronousDuration, 2)
        + "x zo snel als synchroon houtzagen.")

    System.exit(0)
  }
}

object SynchronousSawmill extends Sawmill {
  override def saw(logs: List[Log]): List[Plank] =
    logs.map(saw)
}

object AsynchronousSawmill extends Sawmill {
  implicit val executionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(aantalThreads))

  override def saw(logs: List[Log]): List[Plank] = {
    val future = Future.sequence(logs.map(sawAsync))

    Await.result(future, Duration.Inf)
  }

  private def sawAsync(log: Log): Future[Plank] =
    Future {
      saw(log)
    }
}

trait Sawmill {
  def saw(logs: List[Log]): List[Plank]

  final def saw(log: Log): Plank = {
    Thread.sleep(zaagTijdMs)
    println(s" - $log gezaagd.")
    new Plank(log.id)
  }
}

class Log(val id: Int)

class Plank(val id: Int)

object AsynchronyHelper {
  def executionTime(runnable: () => Any): Double = {
    val start = System.currentTimeMillis()
    runnable()
    val durationMs = System.currentTimeMillis() - start
    durationMs / 1000.0
  }

  def divide(a: Double, b: Double, precision: Int): Double =
    BigDecimal(a / b).setScale(precision, BigDecimal.RoundingMode.HALF_UP).toDouble
}
