package dev.angerm.ag_server.http

import com.linecorp.armeria.server.annotation.Get
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import java.io.StringWriter

class PrometheusHandler(private val registry: CollectorRegistry) : HttpHandler {
    override val pathPrefix: String
        get() = "/"

    init {
        DefaultExports.initialize()
    }

    @Get("/metrics")
    fun get(): String {
        val writer = StringWriter()
        TextFormat.write004(writer, registry.metricFamilySamples())
        return writer.toString()
    }
}