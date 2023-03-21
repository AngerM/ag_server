package dev.angerm.ag_server.http

import com.linecorp.armeria.common.HttpHeaderNames
import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.common.MediaType
import com.linecorp.armeria.server.annotation.Get
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import java.io.StringWriter
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap

class PrometheusHandler(private val registry: CollectorRegistry) : HttpHandler {
    override val pathPrefix: String
        get() = "/"

    companion object {
        val registered = ConcurrentHashMap<CollectorRegistry, Boolean>()
    }

    init {
        registered.computeIfAbsent(registry) {
            DefaultExports.register(it)
            true
        }
    }

    private fun parseQuery(query: String?): Set<String> {
        val names: MutableSet<String> = HashSet()
        val pairs = query?.split("&")?.toTypedArray().orEmpty()
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx != -1 && URLDecoder.decode(pair.substring(0, idx), "UTF-8") == "name[]") {
                names.add(URLDecoder.decode(pair.substring(idx + 1), "UTF-8"))
            }
        }
        return names
    }

    @Get("/metrics")
    fun handle(req: HttpRequest): HttpResponse {
        val writer = StringWriter()
        val accept = req.headers().get(HttpHeaderNames.ACCEPT)
        val format = TextFormat.chooseContentType(accept)
        val query = req.uri().rawQuery
        TextFormat.writeFormat(
            format,
            writer,
            registry.filteredMetricFamilySamples(
                parseQuery(query),
            ),
        )
        return HttpResponse.of(
            HttpStatus.OK,
            MediaType.parse(format),
            writer.toString(),
        )
    }
}