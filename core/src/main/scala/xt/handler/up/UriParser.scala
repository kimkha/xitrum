package xt.handler.up

import xt.Logger
import xt.handler.Env
import xt.vc.{Env => CEnv}
import xt.vc.env.PathInfo

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.{HttpRequest, QueryStringDecoder}

@Sharable
class UriParser extends SimpleChannelUpstreamHandler with Logger {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[Env]) {
      ctx.sendUpstream(e)
      return
    }

    val env          = m.asInstanceOf[Env]
    val request      = env("request").asInstanceOf[HttpRequest]
    val decoder      = new QueryStringDecoder(request.getUri)
    env("pathInfo")  = new PathInfo(decoder.getPath)
    env("uriParams") = decoder.getParameters
    Channels.fireMessageReceived(ctx, env)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("UriParser", e.getCause)
    e.getChannel.close
  }
}
