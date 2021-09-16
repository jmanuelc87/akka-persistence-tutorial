package practices

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.persistence.journal.{EventSeq, ReadEventAdapter, WriteEventAdapter}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable

object EventAdaptersDemo extends App {

  // store
  val ACOUSTIC = "acoustic"
  val ELECTRIC = "electric"

  // online store for acoustic guitars
  case class Guitar(id: String, model: String, make: String, guitarType: String = ACOUSTIC)

  case class AddGuitar(guitar: Guitar, quantity: Int)

  case class GuitarAdded(guitarId: String, guitarModel: String, guitarMake: String, quantity: Int)

  case class GuitarAddedV2(guitarId: String, guitarModel: String, guitarMake: String, quantity: Int, guitarType: String = ACOUSTIC)

  class InventoryManager extends PersistentActor with ActorLogging {
    override def persistenceId: String = "guitar-inventory-manager"

    val inventory: mutable.Map[Guitar, Int] = new mutable.HashMap[Guitar, Int]()


    override def receiveCommand: Receive = {
      case AddGuitar(guitar@Guitar(id, model, make, guitarType), quantity) =>
        persist(GuitarAdded(id, model, make, quantity)) {
          e =>
            addGuitarInventory(guitar, quantity)
            log.info(s"Added $quantity x $guitar to inventory")
        }

      case "print" =>
        log.info(s"Current inventory is: $inventory")
    }

    override def receiveRecover: Receive = {
      case GuitarAddedV2(id, guitarModel, guitarMake, quantity, guitarType) =>
        val guitar = Guitar(id, guitarModel, guitarMake, guitarType)
        addGuitarInventory(guitar, quantity)
    }

    def addGuitarInventory(guitar: Guitar, quantity: Int) = {
      val exisingQuantity = inventory.getOrElse(guitar, 0)
      inventory.put(guitar, exisingQuantity + quantity)
    }
  }

  class GuitarReadEventAdapter extends ReadEventAdapter {
    /**
      * journal -> serializer -> read event adapter -> actor
      * (bytes)    (GA)          (GAv2)                (receiveRecover)
      */

    override def fromJournal(event: Any, manifest: String): EventSeq = event match {
      case GuitarAdded(id, model, make, quantity) =>
        EventSeq.single(GuitarAddedV2(id, model, make, quantity, ACOUSTIC))

      case other => EventSeq.single(other)
    }
  }

  class GuitarWriteEventAdapter extends WriteEventAdapter {
    override def manifest(event: Any): String = ""

    override def toJournal(event: Any): Any = event match {
      case other => EventSeq.single(other)
    }
  }

  val system = ActorSystem("eventAdapters", ConfigFactory.load().getConfig("eventAdapters"))
  val inventoryActor = system.actorOf(Props[InventoryManager], "inventoryMananger")

  val guitars = for (i <- 1 to 10) yield Guitar(s"$i", s"HaKker ${i}", "RockTheJVM")

  //  guitars.foreach {
  //    guitar =>
  //      inventoryActor ! AddGuitar(guitar, 5)
  //  }

  inventoryActor ! "print"
}
