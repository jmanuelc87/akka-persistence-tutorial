package practices

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.persistence.journal.{EventAdapter, EventSeq}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable

object DomainModel {
  case class User(id: String, email: String, name: String)

  case class Coupon(code: String, promotionAmount: Int)

  // command
  case class ApplyCoupon(user: User, coupon: Coupon)

  // event
  case class CouponApplied(code: String, user: User)
}

object DataModel {
  case class WrittenCouponApplied(
                                   code: String,
                                   userId: String,
                                   userMail: String
                                 )

  case class WrittenCouponAppliedV2(
                                     code: String,
                                     userId: String,
                                     userMail: String,
                                     userName: String
                                   )
}

class ModelAdapter extends EventAdapter {

  import DataModel._
  import DomainModel._

  def manifest(event: Any): String = "CMA"

  def toJournal(event: Any): Any = event match {
    case event@CouponApplied(code, user) =>
      WrittenCouponAppliedV2(code, user.id, user.email, user.name)
  }

  def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case event@WrittenCouponApplied(code, userId, userMail) =>
      EventSeq.single(CouponApplied(code, User(userId, userMail, "")))

    case event@WrittenCouponAppliedV2(code, userId, userMail, userName) =>
      EventSeq.single(CouponApplied(code, User(userId, userMail, userName)))

    case other =>
      EventSeq.single(other)
  }

}

object DetachingModels extends App {

  class CouponManager extends PersistentActor with ActorLogging {

    import DomainModel._

    val coupons: mutable.Map[String, User] = new mutable.HashMap[String, User]

    override def persistenceId: String = "akka-coupon-manager"

    override def receiveRecover: PartialFunction[Any, Unit] = {
      case event@CouponApplied(code, user) =>
        log.info(s"Recovered $event")
        coupons.put(code, user)
    }

    override def receiveCommand: PartialFunction[Any, Unit] = {
      case ApplyCoupon(user, coupon) =>
        if (!coupons.contains(coupon.code)) {
          persist(CouponApplied(coupon.code, user)) { e =>
            log.info(s"Persisted $e")
            coupons.put(coupon.code, user)
          }
        }
    }
  }

  val system = ActorSystem(
    "DeatachingModels",
    ConfigFactory.load().getConfig("deatachingModels")
  )
  val couponManager = system.actorOf(Props[CouponManager], "couponManager")

  for (i <- 16 to 20) {
    val coupon = DomainModel.Coupon(s"Mega_Coupon_$i", 100)
    val user = DomainModel.User(s"$i", s"user_$i@rtjvm.com", s"John Doe $i")

    couponManager ! DomainModel.ApplyCoupon(user, coupon)
  }

}
