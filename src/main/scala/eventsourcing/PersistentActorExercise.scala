package eventsourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.persistence.PersistentActor
import akka.util.Timeout

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.Success


object PersistentActorExercise extends App {

  /**
    * Persistent Actor for a voting system
    * Keep:
    *   - the citizens who voted
    *   - the poll: mapping between a candidate and the number of received votes so far
    *
    * The actor must be able to recover its state if shutdown or restarted
    *
    * Vote class is the entry command for voting for a citizen vote for a candidate
    * Winner class is the command for showing the poll result
    */
  case class Vote(id: String, candidate: String)

  case object WhoWin

  case class Winner(candidate: String, votes: BigInt)

  case class VoteEvent(id: String, candidate: String, date: LocalDateTime)

  /**
    * Entry Actor
    */
  class Poll extends PersistentActor with ActorLogging {

    override def persistenceId: String = "poll"

    override def receiveRecover: Receive = {
      case VoteEvent(id, candidate, date) =>
        log.info(s"A vote occurred in $date")
        vote(id, candidate)
    }

    override def receiveCommand: Receive = {
      case Vote(id, candidate) =>
        voted(id, candidate)

      case WhoWin =>
        val who = winner()
        sender() ! Winner(who._1, who._2)
    }

    var poll: Map[String, BigInt] = Map.empty[String, BigInt]

    var citizenVoted: Vector[String] = Vector.empty[String]

    private def voted(id: String, candidate: String): Unit = {
      if (!citizenVoted.contains(id)) {
        log.info("A citizen voted!")
        val command = VoteEvent(id, candidate, LocalDateTime.now())
        persist(command) {
          event =>
            vote(id, candidate)
        }
      } else {
        log.warning(s"The citizen $id already voted!")
      }
    }

    private def vote(id: String, candidate: String): Unit = {
      val entry = poll.getOrElse(candidate, BigInt(0))
      poll = poll.updated(candidate, entry + 1)
      citizenVoted = citizenVoted :+ id
    }

    private def winner(): (String, BigInt) = {
      poll.foldLeft(("", BigInt(0))) {
        case (entry, acc) =>
          if (acc._2 < entry._2) {
            entry
          } else {
            acc
          }
      }
    }
  }

  implicit val timeout: Timeout = Timeout(1.second)

  val system = ActorSystem("persistent-voting-system")

  sys.addShutdownHook(system.terminate())

  val pollActor = system.actorOf(Props[Poll], "poll")

  pollActor ! Vote("1", "Juan")
  pollActor ! Vote("2", "Juan")
  pollActor ! Vote("3", "Juan")

  val who = pollActor ? WhoWin

  who.onComplete {
    case Success(Winner(candidate, votes)) =>
      println(s"The candidate: $candidate is winning at this moment: ${LocalDateTime.now()} with an amount of votes ${votes}")
  }

  pollActor ! Vote("4", "Jose")
  pollActor ! Vote("5", "Jose")
  pollActor ! Vote("6", "Jose")
  pollActor ! Vote("7", "Jose")

  val who2 = pollActor ? WhoWin

  who2.onComplete {
    case Success(Winner(candidate, votes)) =>
      println(s"The candidate: $candidate is winning at this moment: ${LocalDateTime.now()} with an amount of votes ${votes}")
  }

  pollActor ! Vote("8", "Alberto")
  pollActor ! Vote("9", "Alberto")
  pollActor ! Vote("10", "Alberto")
  pollActor ! Vote("11", "Alberto")
  pollActor ! Vote("12", "Alberto")

  val who3 = pollActor ? WhoWin

  who3.onComplete {
    case Success(Winner(candidate, votes)) =>
      println(s"The candidate: $candidate is winning at this moment: ${LocalDateTime.now()} with an amount of votes ${votes}")
  }

  pollActor ! Vote("12", "Alberto")
}
