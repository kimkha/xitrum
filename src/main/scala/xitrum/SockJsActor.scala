package xitrum

import akka.actor.{Actor, ActorRef}

import xitrum.handler.{DefaultHttpChannelPipelineFactory, HandlerEnv}
import xitrum.sockjs.{CloseFromHandler, MessageFromHandler}

//------------------------------------------------------------------------------

case class SockJsText(text: String)

/**
 * An actor will be created when there's new SockJS session. It will be stopped when
 * the session is closed.
 *
 * Note that unlike WebSocketActor and ActionActor, SockJsActor does not extend
 * Action, see the explanation:
 * https://github.com/sockjs/sockjs-node#various-issues-and-design-considerations
 */
trait SockJsActor extends Actor with Logger {
  // ActorRef of NonWebSocketSession, SockJSWebsocket, or SockJSRawWebsocket
  private[this] var sessionActorRef: ActorRef = _

  def receive = {
    case sessionActorRef: ActorRef =>
      this.sessionActorRef = sessionActorRef
      execute()
  }

  /**
   * The current action is the one just before switching to this SockJS actor.
   * You can extract session data, request headers etc. from it, but do not use
   * respondText, respondView etc. Use respondSockJsText and respondSockJsClose.
   */
  def execute()

  //----------------------------------------------------------------------------

  def respondSockJsText(text: String) {
    sessionActorRef ! MessageFromHandler(text)
  }

  def respondSockJsClose() {
    // For non-WebSocket session, until the timeout occurs, the server must serve the close message
    sessionActorRef ! CloseFromHandler
  }
}
