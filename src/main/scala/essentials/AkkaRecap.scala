package essentials

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, PoisonPill, Props, Stash, SupervisorStrategy}
import akka.util.Timeout

object AkkaRecap extends App {

  class SimpleActor extends Actor with ActorLogging with Stash {

    override def receive: Receive = {
      case "createChild" =>
        val childActor = context.actorOf(Props[SimpleActor], "simpleActor")
        childActor ! "Hello there"
      case "stashThis" =>
        stash();
      case "unstashThis" =>
        unstashAll()
        context.become(anotherHandler)
      case "change" => context.become(anotherHandler)
      case message => println(s"I received: $message")
    }

    def anotherHandler: Receive = {
      case message => println(s"In another receive handler: $message")
    }

    override def preStart(): Unit = log.info("I'm starting")

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: RuntimeException => Restart
      case _ => Stop
    }
  }

  val system = ActorSystem("AkkaRecap")
  // creating an actor
  val actor = system.actorOf(Props[SimpleActor], "simpleActor")
  // sending a message to an actor
  actor ! "Hello!"

  /**
    * - messages are sent asynchronously
    * - many actors (in the millions) can share a few dozen threads
    * - each message is processed/handled atomically
    * - no need for locks
    */

  // changing actor behavior + stashing
  // actors can spawn other actors
  // guardians: /system, /user, / = root guardian

  // actors have a defined lifecycle: they can be started, stopped, suspended, resumed, restarted

  // stopping actors - context.stop
  actor ! PoisonPill

  // logging trait
  // supervision

  // configure akka infrastructure: dispatchers, routers, mailboxes

  // schedulers

  import system.dispatcher

  import scala.concurrent.duration._

  system.scheduler.scheduleOnce(2 seconds) {
    actor ! "delayed hello, world!"
  }

  // Akka patterns including FSM + ask pattern

  import akka.pattern.ask

  implicit val timeout = Timeout(3 seconds)

  val future = actor ? "question"

  // the pipe pattern

  import akka.pattern.pipe

  val anotherActor = system.actorOf(Props[SimpleActor], "anotherSimpleActor")

  future.mapTo[String].pipeTo(anotherActor)
}
