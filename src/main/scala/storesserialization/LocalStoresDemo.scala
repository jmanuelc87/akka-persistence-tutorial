package storesserialization

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import com.typesafe.config.ConfigFactory

object LocalStoresDemo extends App {

  class SimplePersistentActor extends PersistentActor with ActorLogging {

    override def persistenceId: String = "simple-persistence-actor"

    var nMessages = 0

    override def receiveCommand: Receive = {
      case "print" =>
        log.info(s"I have persisted $nMessages")

      case "snap" =>
        saveSnapshot(nMessages)
      case SaveSnapshotSuccess(metadata) =>
        log.info(s"Save snapshot was successful: $metadata")
      case SaveSnapshotFailure(_, cause) =>
        log.info(s"Save snapshot failed: $cause")
      case message =>
        persist(message) {
          e =>
            log.info(s"Persisting $message")
            nMessages += 1
        }
    }

    override def receiveRecover: Receive = {
      case SnapshotOffer(metadata, payload: Int) =>
        log.info(s"Recovered snapshot: $payload")
        nMessages = payload

      case message =>
        log.info(s"Recovered $message")
        nMessages += 1
    }
  }

  val localStoresActorSystem = ActorSystem("localStoresSystem", ConfigFactory.load().getConfig("localStores"))

  val persistentActor = localStoresActorSystem.actorOf(Props[SimplePersistentActor], "simplePersistentActor")

  for (i <- 1 to 10) {
    persistentActor ! s"I love Akka [$i]"
  }

  persistentActor ! "print"
  persistentActor ! "snap"

  for (i <- 11 to 20) {
    persistentActor ! s"I love Akka [$i]"
  }
}
