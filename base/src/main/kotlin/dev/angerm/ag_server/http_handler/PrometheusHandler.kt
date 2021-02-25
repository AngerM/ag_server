package dev.angerm.ag_server.http_handler

import com.linecorp.armeria.server.annotation.Get
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import java.io.StringWriter

class PrometheusHandler : HttpHandler {
    override val pathPrefix: String
        get() = "/"

    init {
        DefaultExports.initialize()
    }

    @Get("/prometheus")
    fun get(): String {
        val writer = StringWriter()
        TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples())
        return writer.toString()
    }
}
