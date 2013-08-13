import akka.actor.Actor
import akka._
import akka.actor.{ ActorSystem, Actor, Props }
import akka.actor._
import akka.remote._
import com.typesafe.config.ConfigFactory
import akka.pattern.ask
import akka.util.Timeout
import akka.event.Logging
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._
import scala.concurrent.Future
import akka.pattern.{ ask, pipe }
import scala.util.{Try, Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.stm._
import scala.concurrent._

sealed trait Event
case class Login      (user: String)      extends Event
case class Logout     (user: String)      extends Event
case class GetChatLog (from: String)      extends Event
case class ChatLog    (log: List[String]) extends Event
case class ChatMessage(from: String, message: String) extends Event

object ChatActorSystem {
  val system = ActorSystem("ChatService", ConfigFactory.load.getConfig("chatter"))
}

trait ChatSystem {
  val system = ChatActorSystem.system
}

class ChatClient(val name: String) extends ChatSystem{
  val remotePath = "akka.tcp://ChatService@127.0.0.1:2553/user/receiver"
  val chat = system.actorFor( remotePath ) 
  
  implicit val timeout = Timeout(1 minutes)

  def login                 = chat ! Login(name)
  def logout                = chat ! Logout(name)
  def post(message: String) = chat ! ChatMessage(name, name + ": " + message)
  def chatLog               = Await.result((chat ? GetChatLog(name)).mapTo[ChatLog], 1 minute)
}

class Session( user: String, storage: ActorRef ) extends Actor {
  private val loginTime = System.currentTimeMillis
  private val userLog: List[String] = Nil

  // log.debug("New session for user [%s] has been created at [%s]".format(user, loginTime))

  def receive = {
    case msg @ ChatMessage(from, message) =>
      message :: userLog
      storage ! msg
 
    case msg @ GetChatLog(_) =>
      storage forward msg
  }
}

trait ChatServer extends Actor with ChatSystem{ 
  val system: ActorSystem

  override val supervisorStrategy =
  OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _ ⇒ Restart
  }
  val storage: ActorRef

  def receive: Receive = sessionManagement orElse chatManagement

  protected def chatManagement    : Receive
  protected def sessionManagement : Receive
  protected def shutdownSessions(): Unit

  override def postStop() = {
    println("Chat server is shutting down...")
    shutdownSessions
    system.stop(storage)
  }

}

trait SessionManagement { this: Actor =>
  val system: ActorSystem

  val storage: ActorRef
  var sessions = Map[String, ActorRef]()

  protected def sessionManagement: Receive = {
    case Login(username) => 
      println( "User " + username + " has logged in.")
      val session = system.actorOf( Props(classOf[Session], username, storage), name = username)
      sessions = sessions ++ Map(username -> session)
      println( sessions ) 
    
    case Logout(username) => 
      println( "User " + username + " has logged out." )
      val session = sessions(username)
      system.stop(session)
      sessions = sessions - username
      println( sessions )
    }

  protected def shutdownSessions = 
    sessions.foreach { case (_, session) => system.stop(session) }
}

trait ChatManagement { this: Actor =>
  var sessions: Map[String, ActorRef]

  protected def chatManagement: Receive = {
    case msg @ ChatMessage(from, _) => getSession(from).foreach(_ ! msg)
    case msg @ GetChatLog(from)     => getSession(from).foreach(_ forward msg)
  }

  private def getSession(from: String): Option[ActorRef] = {
    if (sessions.contains(from))
      Some(sessions(from))
    else {
      println( "Session has exprired for " + from )
      None
    }
  }
}

trait ChatStorage extends Actor

class MemoryChatStorage extends ChatStorage {

  private var chatLog = Vector[String]()

  def receive = {
    case ChatMessage(from, message) =>
      println("New chat message : " + message )
      atomic { implicit txn => chatLog = chatLog :+ message; chatLog }
    case GetChatLog(_) =>
      val messageList = atomic { implicit txn => chatLog.toList }
      Future{ ChatLog(messageList) } pipeTo sender  
  }

  override def postRestart( reason: Throwable ) = chatLog = Vector()
}

trait MemoryChatStorageFactory extends ChatSystem { 
  val system: ActorSystem
  val storage = system.actorOf(Props[MemoryChatStorage])
}

class ChatService extends ChatServer 
with SessionManagement 
with ChatManagement 
with MemoryChatStorageFactory 
with ChatSystem

object Main extends App with ChatSystem{
  system.actorOf(Props[ChatService], name = "receiver")

  val client1 = new ChatClient("jonas")
  client1.login
  val client2 = new ChatClient("patrik")
  client2.login

  client1.post("Hi there")
  println("CHAT LOG:\n\t" + client1.chatLog.log)

  client2.post("Hello")
  println("CHAT LOG:\n\t" + client2.chatLog.log)

  client1.post("Hi again")

  client1.logout
  client2.logout

  system.shutdown
}


