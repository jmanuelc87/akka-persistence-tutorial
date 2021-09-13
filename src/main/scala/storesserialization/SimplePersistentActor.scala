package storesserialization

import akka.actor.ActorLogging
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}

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
