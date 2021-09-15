package storesserialization

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.serialization.Serializer
import com.typesafe.config.ConfigFactory

case class RegisterUser(email: String, name: String)

case class UserRegistered(id: Int, email: String, name: String)

// serializer
class UserRegistrationSerializer extends Serializer {

  override def identifier: Int = 123456

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event@UserRegistered(id, email, name) =>
      s"[$id\t$email\t$name]".getBytes()
  }

  override def includeManifest: Boolean = false

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val string = new String(bytes)
    val values = string.substring(1, string.length() - 1).split("\t")
    val id = values(0).toInt
    val email = values(1)
    val name = values(2)

    UserRegistered(id, email, name)
  }
}

class UserRegistrationActor extends PersistentActor with ActorLogging {

  var currentId = 0

  override def persistenceId: String = "user-registration"

  override def receiveCommand: Receive = {
    case RegisterUser(email, name) =>
      persist(UserRegistered(currentId, email, name)) {
        e =>
          currentId += 1
          log.info(s"Persisted $e")
      }
  }

  override def receiveRecover: Receive = {
    case event@UserRegistered(id, _, _) =>
      log.info(s"Recovered $event")
      currentId = id
  }
}

object CustomSerialization extends App {

  val system = ActorSystem("customSerializerDemo", config = ConfigFactory.load().getConfig("customSerializerDemo"))

  val userActor = system.actorOf(Props[UserRegistrationActor], "userRegistration")

  for (i <- 1 to 10) {
    userActor ! RegisterUser("jm.carb@gmail.com", "Juan Manuel Carballo")
  }
}
