package eventsourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}

import java.time.LocalDateTime
import scala.util.Random

object MultiplePersistentActorsDemo extends App {

  case class BalanceSnapshot(id: String, amount: BigDecimal, timestamp: LocalDateTime)

  case class Withdraw(id: String, amount: BigDecimal, timestamp: LocalDateTime)

  case class Deposit(id: String, amount: BigDecimal, timestamp: LocalDateTime)


  case class WithdrawEvent(id: String, amount: BigDecimal, timestamp: LocalDateTime)

  case class DepositEvent(id: String, amount: BigDecimal, timestamp: LocalDateTime)

  case class BankAccountActor(id: Integer) extends PersistentActor with ActorLogging {

    val MAX_TRANSACTIONS = 100

    var accountBalance: BigDecimal = 0.0

    var transactionsWithoutCheckpoint = 0

    override def persistenceId: String = s"bank-account-$id"

    override def receiveCommand: Receive = {
      case Deposit(pid, amount, timestamp) =>
        if (pid.equalsIgnoreCase(persistenceId)) {
          persist(DepositEvent(persistenceId, amount, timestamp)) {
            e =>
              accountBalance += e.amount
              log.info(s"Deposit, Account balance $accountBalance")
              maybeCheckpoint()
          }
        }

      case Withdraw(pid, amount, timestamp) =>
        if (pid.equalsIgnoreCase(persistenceId)) {
          if (accountBalance > 0) {
            persist(WithdrawEvent(persistenceId, amount, timestamp)) {
              e =>
                accountBalance -= e.amount
                log.info(s"Withdraw, Account balance $accountBalance")
                maybeCheckpoint()
            }
          }
        }
    }

    override def receiveRecover: Receive = {
      case DepositEvent(pid, amount, timestamp) =>
        if (pid.equalsIgnoreCase(persistenceId)) {
          accountBalance += amount
          log.info(s"Deposit, Account balance $accountBalance")
        }

      case WithdrawEvent(pid, amount, timestamp) =>
        if (pid.equalsIgnoreCase(persistenceId)) {
          if (accountBalance > 0) {
            accountBalance -= amount
            log.info(s"Withdraw, Account balance $accountBalance")
          }
        }

      case SnapshotOffer(metadata, contents) =>
        val pid = contents.asInstanceOf[BalanceSnapshot].id

        if (pid.equalsIgnoreCase(persistenceId)) {
          accountBalance = contents.asInstanceOf[BalanceSnapshot].amount
        }
    }

    def maybeCheckpoint(): Unit = {
      transactionsWithoutCheckpoint += 1
      if (transactionsWithoutCheckpoint >= MAX_TRANSACTIONS) {
        log.info("Saving checkpoints")
        saveSnapshot(BalanceSnapshot(persistenceId, accountBalance, LocalDateTime.now()))
        transactionsWithoutCheckpoint = 0
      }
    }
  }

  val system = ActorSystem("bank-account-system")

  val account1 = system.actorOf(Props(BankAccountActor(1)), "account-1")
  val account2 = system.actorOf(Props(BankAccountActor(2)), "account-2")

  val rand = new Random()

  account1 ! Deposit("bank-account-1", 1000, LocalDateTime.now())
  account2 ! Deposit("bank-account-2", 1000, LocalDateTime.now())

  for (i <- 1 to 1000) {
    if (rand.nextBoolean()) {
      account1 ! Deposit("bank-account-1", rand.nextDouble() * 100, LocalDateTime.now())
      account2 ! Deposit("bank-account-2", rand.nextDouble() * 100, LocalDateTime.now())
    } else {
      account1 ! Withdraw("bank-account-1", rand.nextDouble() * 10, LocalDateTime.now())
      account2 ! Withdraw("bank-account-2", rand.nextDouble() * 10, LocalDateTime.now())
    }
  }

}
