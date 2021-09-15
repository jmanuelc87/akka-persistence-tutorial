package storesserialization

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object CassandraDemo extends App {
  val cassandraStoresActorSystem = ActorSystem("localStoresSystem", ConfigFactory.load().getConfig("cassandraDemo"))

  val persistentActor = cassandraStoresActorSystem.actorOf(Props[SimplePersistentActor], "simplePersistentActor")

//  for (i <- 1 to 1000) {
//    persistentActor ! s"I love Akka [$i]"
//  }

  persistentActor ! "print"
//  persistentActor ! "snap"

//  for (i <- 1001 to 2000) {
//    persistentActor ! s"I love Akka [$i]"
//  }
}
