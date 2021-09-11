package eventsourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}

import scala.collection.mutable

object Snapshots extends App {

  // commands
  case class ReceivedMessage(content: String)

  case class SentMessage(contents: String)

  // events
  case class ReceivedMessageRecord(id: Int, contents: String)

  case class SentMessageRecord(id: Int, contents: String)


  object Chat {
    def props(owner: String, contact: String) = Props(new Chat(owner, contact))
  }

  class Chat(owner: String, contact: String) extends PersistentActor with ActorLogging {

    val MAX_MESSAGES = 10

    val lastMessages = new mutable.Queue[(String, String)]()

    var currentMessageId = 0

    var commandsWithoutCheckpoint = 0

    override def persistenceId: String = s"$owner-$contact-chat"

    override def receiveCommand: Receive = {
      case ReceivedMessage(content) =>
        persist(ReceivedMessageRecord(currentMessageId, content)) {
          e =>
            log.info(s"Received message: $content")
            maybeReplaceMessage(contact, content)
            currentMessageId += 1
            maybeCheckpoint()
        }

      case SentMessage(contents) =>
        persist(SentMessageRecord(currentMessageId, contents)) {
          e =>
            log.info(s"Sent message: $contents")
            maybeReplaceMessage(owner, contents)
            currentMessageId += 1
            maybeCheckpoint()
        }

      case "print" =>
        log.info(s"Most recent messages $lastMessages")

      case SaveSnapshotSuccess(metadata) =>
        log.info(s"saving snapshot succeeded: $metadata")

      case SaveSnapshotFailure(metadata, reason) =>
        log.info(s"saving snapshot failure: $metadata, $reason")
    }

    override def receiveRecover: Receive = {
      case ReceivedMessageRecord(id, contents) =>
        log.info(s"Recovered Receive Message $id")
        maybeReplaceMessage(contact, contents)
        currentMessageId = id

      case SentMessageRecord(id, contents) =>
        log.info(s"Recovered Sent Message $id")
        maybeReplaceMessage(owner, contents)
        currentMessageId = id

      case SnapshotOffer(metadata, contents) =>
        log.info(s"Recovered Snapshot: $metadata")
        contents.asInstanceOf[mutable.Queue[(String, String)]].foreach(lastMessages.enqueue(_))
    }

    def maybeReplaceMessage(sender: String, contents: String): Unit = {
      if (lastMessages.size >= MAX_MESSAGES) {
        lastMessages.dequeue()
      }
      lastMessages.enqueue((sender, contents))
    }

    def maybeCheckpoint(): Unit = {
      commandsWithoutCheckpoint += 1
      if (commandsWithoutCheckpoint >= MAX_MESSAGES) {
        log.info("Saving checkpoints")
        saveSnapshot(lastMessages) // asynchronous operation
        commandsWithoutCheckpoint = 0
      }
    }
  }

  val system = ActorSystem("SnapshotsDemo")
  val chat = system.actorOf(Chat.props("Juan123", "Carla123"))

  //  for (i <- 1 to 100000) {
  //    chat ! ReceivedMessage(s"Akka Rocks $i")
  //    chat ! SentMessage(s"Akka Rules $i")
  //  }

  chat ! "print"
}
