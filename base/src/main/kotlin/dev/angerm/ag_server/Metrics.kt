package dev.angerm.ag_server

import com.google.inject.Inject
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.Histogram

class Metrics @Inject constructor(registry: CollectorRegistry) {
    val httpCounter = Counter.Builder()
        .name("http_request")
        .help("http request counters")
        .labelNames(
            "http_method",
            "request_path",
            "status_code"
        ).register(registry)

    val httpLatency = Histogram.Builder()
        .name("http_latency")
        .help("http request latency in ms")
        .exponentialBuckets(1.0, 2.0, 20)
        .labelNames(
            "http_method",
            "request_path",
            "status_code"
        ).register(registry)
}