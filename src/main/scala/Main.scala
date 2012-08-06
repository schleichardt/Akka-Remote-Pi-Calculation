
package info.schleichardt.learning.akka


import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.routing.RoundRobinRouter
import akka.util.Duration
import akka.util.duration._



//sbt "run-main info.schleichardt.learning.akka.LocalApp"
//sbt "run-main info.schleichardt.learning.akka.RemoteApp"
//TODO: Shut down actors correctyl
//TODO: Ask more appropriate?
//TODO: How to test with Akka?


object Configuration {
  val MiddleManName = "remoteMiddleman"
  val NameRemoteSystem = "RemoteSystem"
  val Host = "127.0.0.1"
  val RemotePort = 2553
}


object LocalApp extends App {
  import Configuration._
  val system = ActorSystem("LocalSystem", ConfigFactory.load.getConfig("local"))
  val remoteActor = {
    val actorPath = "akka://%s@%s:%s/user/%s".format(NameRemoteSystem, Host, RemotePort, MiddleManName)
    val remoteActor = system.actorFor(actorPath)
    val listener = system.actorOf(Props[Listener])
    remoteActor.tell(Calculate(10000), listener)
  }
}

object RemoteApp extends App {
  import Configuration._
  val system = ActorSystem(NameRemoteSystem, ConfigFactory.load.getConfig("remote"))
  val actor = system.actorOf(Props(new MiddleMan()), MiddleManName)
}

class MiddleMan extends Actor {
  def receive = {
    case message =>
      val nrOfMessages = 10000
      val nrOfWorkers = 4
      val senderClosure = sender //why does not work sender directly?
      val master = context.actorOf(Props(new Master(nrOfWorkers, nrOfMessages, senderClosure)))
      master forward message //you can change the content (even the type of the message changes when forwarding)
  }
}

class Worker extends Actor {

  def calculatePiFor(start: Int, nrOfElements: Int): Double = {
    var acc = 0.0
    for (i <- start until (start + nrOfElements))
      acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)
    acc
  }

  def receive = {
    case Work(start, nrOfElements) => sender ! Result(calculatePiFor(start, nrOfElements))
  }
}

class Master(nrOfWorkers: Int, nrOfMessages: Int, l: ActorRef) extends Actor {
  var pi: Double = _
  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  val workerRouter = context.actorOf(
    Props[Worker].withRouter(RoundRobinRouter(nrOfWorkers)), name = "workerRouter")

  def receive = {
    case Calculate(nrOfElements) =>
      for (i <- 0 until nrOfMessages)
        workerRouter ! Work(i * nrOfElements, nrOfElements)
    case Result(value) =>
      pi += value
      nrOfResults += 1
      if (nrOfResults == nrOfMessages) {
        l ! PiApproximation(pi, duration = (System.currentTimeMillis - start).millis)
        context.stop(self)
        context.system.shutdown()
      }
  }
}

class Listener extends Actor {
  def receive = {
    case message: PiApproximation =>
      println("\n\tPi approximation: \t\t%s\n\tCalculation time: \t%s".format(message.pi, message.duration))
      context.system.shutdown()
  }
}

sealed trait PiMessage
case class Calculate(nrOfDecimalPlaces: Int) extends PiMessage
case class Work(start: Int, nrOfElements: Int) extends PiMessage
case class Result(value: Double) extends PiMessage
case class PiApproximation(pi: Double, duration: Duration)
