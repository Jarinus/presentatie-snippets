import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing._

import LooseCouplingHelpers._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object LooseCoupling {
  implicit val system: ActorSystem = ActorSystem("LooseCoupling")

  def main(args: Array[String]): Unit = {
    val receiver = system.actorOf(MessageReceiverActor.props("Enkele actor"))
    receiver ! Message("Hello world!")

    val receivers = createBroadcastMessageReceiverGroup("Eerste actor", "Tweede actor", "Derde actor")
    receivers ! Message("Hello world!")

    Await.ready(system.terminate(), Duration.Inf)
  }
}

class MessageReceiverActor(name: String) extends Actor {
  def receive: Receive = {
    case Message(content) =>
      println(s"$name heeft '$content' ontvangen")
  }
}

object MessageReceiverActor {
  def props(name: String) = Props(new MessageReceiverActor(name))
}

case class Message(content: String)

object LooseCouplingHelpers {
  def createBroadcastMessageReceiverGroup(names: String*)(implicit system: ActorSystem): ActorRef = {
    val receiverActors = names
      .map(MessageReceiverActor.props)
      .map(system.actorOf)
      .map(_.path.toStringWithoutAddress)
      .toVector

    system.actorOf(BroadcastGroup(receiverActors).props(), "receivers")
  }
}
