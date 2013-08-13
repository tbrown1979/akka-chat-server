import akka.actor.Actor
import akka._
import akka.actor.{ ActorSystem, Actor, Props }
 
class MyActor extends Actor {
  def receive = {
    case "test" => println("received test")
    case _ =>      println("received unknown message")
  }
}

// object Main extends App { 
//   val system = ActorSystem()
//   val myActor = system.actorOf(Props[MyActor])

//   myActor ! "test"
//   system.shutdown
// }