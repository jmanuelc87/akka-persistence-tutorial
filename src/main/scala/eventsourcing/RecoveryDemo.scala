package eventsourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}

object RecoveryDemo extends App {

  case class Command(contents: String)

  case class Event(id: Int, contents: String)

  class RecoveryActor extends PersistentActor with ActorLogging {

    override def persistenceId: String = "recovery-actor"

    override def receiveCommand: Receive = online(0)

    override def receiveRecover: Receive = {
      case RecoveryCompleted =>
        log.info("I have finished recovery")

      case Event(id, contents) =>
        //        if (contents.contains("999"))
        //          throw new RuntimeException("I can't take this anymore!")
        log.info(s"Recovered: $contents. Persisted is  ${if (this.recoveryFinished) "" else "not"} finished")
        context.become(online(id))
      // will not change the event handler during recovery
      // AFTER recovery the normal handler will be the result of all the stacking of context.become
    }

    override def onRecoveryFailure(cause: Throwable, event: Option[Any]): Unit = {
      log.error(s"I failed at recovery ${cause.getMessage}")
      super.onRecoveryFailure(cause, event)
    }

    def online(latestPersistedEventId: Int): Receive = {
      case Command(contents) =>
        persist(Event(latestPersistedEventId, contents)) {
          event =>
            log.info(s"Persisted is  ${if (this.recoveryFinished) "" else "not"} finished")
            context.become(online(latestPersistedEventId + 1))
        }
    }

    //    override def recovery: Recovery = Recovery(toSequenceNr = 100)

    //    override def recovery: Recovery = Recovery(fromSnapshot = SnapshotSelectionCriteria.latest())

    //    override def recovery: Recovery = Recovery.none
  }

  val system = ActorSystem("RecoveryDemo")
  val recoveryActor = system.actorOf(Props[RecoveryActor], "recoveryActor")

  // 1. Stashing commands
  for (i <- 1 to 1000) {
    recoveryActor ! Command(s"command $i")
  }

  // 2. Failure during recovery
  // - onRecoveryFailure + the actor is stopped

  // 3. customizing recovery
  // DO NOT persist more events after a customized _incomplete_ recovery

  // 4. Recovery status or knowing when you're done recovering
  // getting a signal when you're done recovering

  // 5. stateless actors
  //
}
