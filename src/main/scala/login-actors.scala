import akka.actor.Actor
import akka._
import akka.actor.{ ActorSystem, Actor, Props }

sealed trait Event
case class Login(user: String) extends Event
case class Logout(user: String) extends Event

class LoginActor extends Actor {
  def receive = {
    case Login(x) => println("successfully logged in: " + x)
    case Logout(_) => println("logged out")
    case _ => println("received unexpected message")
  }
}

object Main extends App {
  val system = ActorSystem()
  system.actorOf(Props[LoginActor]) ! Login("tinymidget")
}