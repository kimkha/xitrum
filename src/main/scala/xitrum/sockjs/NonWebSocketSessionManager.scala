package xitrum.sockjs

import scala.collection.mutable.{Map => MMap}

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.contrib.pattern.ClusterSingletonManager

import xitrum.Config

case class Lookup(sockJsSessionId: String)
case class LookupOrCreate(sockJsSessionId: String)

object NonWebSocketSessionManager {
  private val NAME = "NonWebSocketSessionManager"

  def start() {
    val prop = ClusterSingletonManager.props(
      singletonProps     = handOverData =>
        Props(classOf[NonWebSocketSessionManager]),
      singletonName      = NAME,
      terminationMessage = PoisonPill,
      role               = None)
    Config.actorSystem.actorOf(prop, NAME)
  }

  def lookup(sockJsSessionId: String) {
    val sel = Config.actorSystem.actorSelection(NAME)
    sel ! Lookup(sockJsSessionId)
  }

  def lookupOrCreate(sockJsSessionId: String) {
    val sel = Config.actorSystem.actorSelection(NAME)
    sel ! LookupOrCreate(sockJsSessionId)
  }
}

/**
 * Cluster singleton actor. Each node runs a NonWebSocketSessionManager. The
 * leader is in charge of creating NonWebSocketSessions as its children. When it
 * dies, all its children should die.
 */
class NonWebSocketSessionManager extends Actor {
  private val sessions = MMap[String, ActorRef]()

  def receive = {
    case LookupOrCreate(sockJsSessionId) =>
      sessions.get(sockJsSessionId) match {
        case None =>
          //val prop = Props(new NonWebSocketSession(self, pathPrefix, this))

        case Some(actorRef) =>
          sender ! actorRef
      }
  }
}
