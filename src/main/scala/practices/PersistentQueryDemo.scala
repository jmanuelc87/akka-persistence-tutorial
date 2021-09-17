package practices

import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

object PersistentQueryDemo extends App {

  val system = ActorSystem("PersistenceQueryDemo", ConfigFactory.load().getConfig("persistenceQuery"))

  val readJournal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  // give me all persistence Ids
  val persistenceIds = readJournal.persistenceIds()

  implicit val materializer = ActorMaterializer()(system)

  persistenceIds.runForeach {
    persistenceId =>
      println(s"Found persistence id: $persistenceId")
  }
}
