package eventsourcing

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor

import java.time.LocalDateTime

object MultiplePersists extends App {

  /**
    * Diligent accountant: with every invoice, will persist two events
    *   - a tax record for the fiscal authority
    *   - an invoice record for personal logs or some auditing authority
    */

  // COMMAND
  case class Invoice(recipient: String, date: LocalDateTime, amount: Int)


  // EVENTS
  case class TaxRecord(taxId: String, recordId: Int, date: LocalDateTime, totalAmount: Int)

  case class InvoiceRecord(invoiceRecord: Int, recipient: String, date: LocalDateTime, amount: Int)

  object DiligentAccountant {
    def props(taxId: String, taxAuthority: ActorRef) = Props(new DiligentAccountant(taxId, taxAuthority))
  }

  // ACTORS
  class DiligentAccountant(taxId: String, taxAuthority: ActorRef) extends PersistentActor with ActorLogging {

    var latestTaxRecord = 0

    override def receiveRecover: Receive = {
      case message =>
        log.info(s"Message: $message")
    }

    override def receiveCommand: Receive = {
      case Invoice(recipient: String, date: LocalDateTime, amount: Int) =>
        // nested persistence
        // 1
        persist(TaxRecord(taxId, latestTaxRecord, date, amount / 3)) { record =>
          taxAuthority ! record
          latestTaxRecord += 1

          // 3
          persist("I hereby declare this tax record to be true and complete.") { declaration =>
            taxAuthority ! declaration
          }
        }

        // 2
        persist(InvoiceRecord(latestTaxRecord, recipient, date, amount)) { invoiceRecord =>
          taxAuthority ! invoiceRecord
          latestTaxRecord += 1

          // 4
          persist("I hereby declare this invoice record to be true.") { declaration =>
            taxAuthority ! declaration
          }
        }
    }

    override def persistenceId: String = "diligent-accountant"
  }

  class TaxAuthority extends Actor with ActorLogging {
    override def receive: Receive = {
      case message =>
        log.info(s"Received $message")
    }
  }

  val system = ActorSystem("MultiplePersistsDemo")
  val taxAuthority = system.actorOf(Props[TaxAuthority], "HMRC")
  val accountant = system.actorOf(DiligentAccountant.props("MX5432_6543", taxAuthority))

  /**
    * The message ordering (***) is guaranteed because persistence is also based on message passing
    */

  accountant ! Invoice("The Sofa company", LocalDateTime.now(), 1500)

  accountant ! Invoice("The supercar company", LocalDateTime.now(), 2000)
}
