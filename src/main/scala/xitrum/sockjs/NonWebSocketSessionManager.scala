package xitrum.sockjs

import scala.collection.mutable.{Map => MMap}

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, Address, PoisonPill, Props, RootActorPath}
import akka.cluster.{Cluster, ClusterEvent}
import akka.contrib.pattern.ClusterSingletonManager

import xitrum.Config

case class Lookup(sockJsActor: ActorRef, sockJsSessionId: String)
case class LookupOrCreate(sockJsActor: ActorRef, sockJsSessionId: String, pathPrefix: String)

object NonWebSocketSessionManager {
  private val MANAGER = "NonWebSocketSessionManager"
  private val PROXY   = "NonWebSocketSessionProxy"

  private val proxy = Config.actorSystem.actorSelection("/user/" + PROXY)

  def start() {
    val managerProps = ClusterSingletonManager.props(
      singletonProps     = handOverData =>
        Props(classOf[NonWebSocketSessionManager]),
      singletonName      = MANAGER,
      terminationMessage = PoisonPill,
      role               = None)
    Config.actorSystem.actorOf(managerProps, MANAGER)

    val proxyProps = Props[NonWebSocketSessionProxy]
    Config.actorSystem.actorOf(proxyProps, PROXY)
  }

  def leaderSelection(leaderAddress: Option[Address]): Option[ActorSelection] =
    leaderAddress map { a =>
      Config.actorSystem.actorSelection(RootActorPath(a) / "user" / MANAGER / MANAGER)
    }

  def lookup(sockJsActor: ActorRef, sockJsSessionId: String) {
    proxy ! Lookup(sockJsActor, sockJsSessionId)
  }

  def lookupOrCreate(sockJsActor: ActorRef, sockJsSessionId: String, pathPrefix: String) {
    proxy ! LookupOrCreate(sockJsActor, sockJsSessionId, pathPrefix)
  }
}

/**
 * Cluster singleton actor. All nodes run NonWebSocketSessionProxy, but only the
 * leader node runs NonWebSocketSessionManager. NonWebSocketSessionManager is in
 * charge of creating NonWebSocketSessions as its children. When it dies, all
 * its children should die.
 */
class NonWebSocketSessionManager extends Actor {
  // Used when the current actor is the leader
  private val sessions = MMap[String, ActorRef]()

  override def preStart() {
    sessions.clear()
  }

  override def postStop() {
    sessions.clear()
  }

  def receive = {
    case Lookup(sockJsActor, sockJsSessionId) =>
      sockJsActor ! sessions.get(sockJsSessionId)

    case LookupOrCreate(sockJsActor, sockJsSessionId, pathPrefix) =>
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

/**
 * All nodes run NonWebSocketSessionProxy, but only the
 * leader node runs NonWebSocketSessionManager. NonWebSocketSessionProxy is for
 * determining where NonWebSocketSessionManager is.
 */
class NonWebSocketSessionProxy extends Actor {
  private var leaderSelection: Option[ActorSelection] = _

  override def preStart() {
    leaderSelection = None
    Cluster(context.system).subscribe(self, classOf[ClusterEvent.LeaderChanged])
  }

  override def postStop() {
    leaderSelection = None
    Cluster(context.system).unsubscribe(self)
  }

  def receive = {
    case state: ClusterEvent.CurrentClusterState =>
      val leaderAddress = Option(state.getLeader)
      leaderSelection = NonWebSocketSessionManager.leaderSelection(leaderAddress)

    case ClusterEvent.LeaderChanged(leaderAddress) =>
      leaderSelection = NonWebSocketSessionManager.leaderSelection(leaderAddress)

    case Lookup(sockJsActor, sockJsSessionId) =>
      leaderSelection.foreach { sel =>
        sel ! Lookup(sockJsActor, sockJsSessionId)
      }

    case LookupOrCreate(sockJsActor, sockJsSessionId, pathPrefix) =>
      leaderSelection.foreach { sel =>
        sel ! LookupOrCreate(sockJsActor, sockJsSessionId, pathPrefix)
      }
  }
}
