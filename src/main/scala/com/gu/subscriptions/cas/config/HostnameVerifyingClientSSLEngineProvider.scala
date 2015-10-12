package com.gu.subscriptions.cas.config
import javax.net.ssl.SSLParameters
import spray.io.ClientSSLEngineProvider

object HostnameVerifyingClientSSLEngineProvider {
    implicit val provider = ClientSSLEngineProvider { engine =>
        val p: SSLParameters = engine.getSSLParameters
        p.setEndpointIdentificationAlgorithm("HTTPS")
        engine.setSSLParameters(p)
        engine
    }
}
