package com.gu.subscriptions.cas.config

import java.net.InetSocketAddress

import akka.actor.{ActorSystem, ActorContext}
import akka.event.{NoLogging, LoggingAdapter}
import org.scalatest.FreeSpec
import spray.io.PipelineContext


class HostnameVerifyingClientSSLEngineProviderTest extends FreeSpec {
    "returns an SSLEngine with hostname verification" in {
        val engine = HostnameVerifyingClientSSLEngineProvider.provider(new PipelineContext {
            override def log: LoggingAdapter = NoLogging
            override def remoteAddress: InetSocketAddress = new InetSocketAddress(443)
            override def actorContext: ActorContext = ???
            override def localAddress: InetSocketAddress = new InetSocketAddress(443)
        })

        for {
            e <- engine
        } yield assertResult(e.getSSLParameters getEndpointIdentificationAlgorithm)("HTTPS")

    }
}
