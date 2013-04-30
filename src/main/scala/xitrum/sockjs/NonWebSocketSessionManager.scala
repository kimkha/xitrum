package xitrum.sockjs

import scala.collection.mutable.{Map => MMap}

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, Address, PoisonPill, Props, RootActorPath}
import akka.cluster.{Cluster, ClusterEvent}
import akka.contrib.pattern.ClusterSingletonManager

import xitrum.Config

case class Lookup(sockJsActor: ActorRef, sockJsSessionId: String)
case class LookupAtLeader(sockJsActor: ActorRef, sockJsSessionId: String)

case class LookupOrCreate(sockJsActor: ActorRef, sockJsSessionId: String, pathPrefix: String)
case class LookupOrCreateAtLeader(sockJsActor: ActorRef, sockJsSessionId: String, pathPrefix: String)

object NonWebSocketSessionManager {
  private val NAME = "NonWebSocketSessionManager"

  private val localManager = Config.actorSystem.actorSelection("/user/" + NAME + "/" + NAME)

  def start() {
    val props = ClusterSingletonManager.props(
      singletonProps     = handOverData =>
        Props(classOf[NonWebSocketSessionManager]),
      singletonName      = NAME,
      terminationMessage = PoisonPill,
      role               = None)
    Config.actorSystem.actorOf(props, NAME)
  }

  def leaderSelection(leaderAddress: Option[Address]): Option[ActorSelection] =
    leaderAddress map { a =>
      Config.actorSystem.actorSelection(RootActorPath(a) / "user" / NAME / NAME)
    }

  def lookup(sockJsActor: ActorRef, sockJsSessionId: String) {
    localManager ! Lookup(sockJsActor, sockJsSessionId)
  }

  def lookupOrCreate(sockJsActor: ActorRef, sockJsSessionId: String, pathPrefix: String) {
    localManager ! LookupOrCreate(sockJsActor, sockJsSessionId, pathPrefix)
  }
}

/**
 * Cluster singleton actor. Each node runs a NonWebSocketSessionManager. The
 * leader is in charge of creating NonWebSocketSessions as its children. When it
 * dies, all its children should die.
 */
class NonWebSocketSessionManager extends Actor {
  // Used when the current actor is the leader
  private val sessions = MMap[String, ActorRef]()

  private var leaderSelection: Option[ActorSelection] = _

  override def preStart() {
    sessions.clear()
    leaderSelection = None
    Cluster(context.system).subscribe(self, classOf[ClusterEvent.LeaderChanged])
  }

  override def postStop() {
    sessions.clear()
    leaderSelection = None
    Cluster(context.system).unsubscribe(self)
  }

  def receive = {
    case state: ClusterEvent.CurrentClusterState =>
      val leaderAddress = Some(state.getLeader)
      leaderSelection = NonWebSocketSessionManager.leaderSelection(leaderAddress)

    case ClusterEvent.LeaderChanged(leaderAddress) =>
      leaderSelection = NonWebSocketSessionManager.leaderSelection(leaderAddress)

    case Lookup(sockJsActor, sockJsSessionId) =>
      leaderSelection.foreach { sel =>
        sel ! LookupAtLeader(sockJsActor, sockJsSessionId)
      }

    case LookupAtLeader(sockJsActor, sockJsSessionId) =>
      sockJsActor ! sessions.get(sockJsSessionId)

    case LookupOrCreate(sockJsActor, sockJsSessionId, pathPrefix) =>
      leaderSelection.foreach { sel =>
        sel ! LookupOrCreateAtLeader(sockJsActor, sockJsSessionId, pathPrefix)
      }

    case LookupOrCreateAtLeader(sockJsActor, sockJsSessionId, pathPrefix) =>
      sessions.get(sockJsSessionId) match {
        case None =>
          val props               = Props(classOf[NonWebSocketSession], sockJsActor, pathPrefix)
          val nonWebSocketSession = context.actorOf(props)

          sessions(sockJsSessionId) = nonWebSocketSession
          sockJsActor ! (true, nonWebSocketSession)

        case Some(nonWebSocketSession) =>
          sockJsActor ! (false, nonWebSocketSession)
      }
  }
}
