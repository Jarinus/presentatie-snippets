import Statelessness._
import StatelessnessHelpers._
import akka.actor.{Actor, ActorSelection, ActorSystem, PoisonPill, Props}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Statelessness {
  val aantalBerichten = 10
  val tijdTussenBerichtenMs = 400

  implicit val system: ActorSystem = ActorSystem("Statelessness")

  def main(args: Array[String]): Unit = {
    system.actorOf(Props[CounterActor], "counter")

    val counter = system.actorSelection("/user/counter")
    val future = sendEverySecondForNSeconds(aantalBerichten, counter, Increment)

    Thread.sleep(aantalBerichten * tijdTussenBerichtenMs / 2)
    counter ! PoisonPill
    Thread.sleep(tijdTussenBerichtenMs)

    system.actorOf(Props[CounterActor], "counter")

    Await.ready(future, Duration.Inf)
    System.exit(0)
  }
}

class CounterActor extends Actor {
  def receive: Receive = receiveStateful(0)

  private def receiveStateful(counter: Int): Receive = {
    case Increment =>
      println(s"Counter: ${counter + 1}")
      context.become(receiveStateful(counter + 1))
  }
}

case object Increment

object StatelessnessHelpers {
  def sendEverySecondForNSeconds(n: Int, recipient: ActorSelection, message: Any): Future[_] = Future {
    for (_ <- 0 until n) {
      Thread.sleep(tijdTussenBerichtenMs)
      recipient ! message
    }
  }(system.dispatcher)
}