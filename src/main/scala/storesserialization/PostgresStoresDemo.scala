package storesserialization

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object PostgresStoresDemo extends App {

  val postgresStoresActorSystem = ActorSystem("postgresStoresSystem", ConfigFactory.load().getConfig("postgresDemo"))

  val persistentActor = postgresStoresActorSystem.actorOf(Props[SimplePersistentActor], "simplePersistentActor")

  for (i <- 1 to 10) {
    persistentActor ! s"I love Akka [$i]"
  }

  persistentActor ! "print"
  persistentActor ! "snap"

  for (i <- 11 to 20) {
    persistentActor ! s"I love Akka [$i]"
  }
}
