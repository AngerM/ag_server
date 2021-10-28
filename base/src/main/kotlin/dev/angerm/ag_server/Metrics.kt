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
        .help("http request latency in seconds")
        .buckets(
            0.001,
            0.005,
            0.010,
            0.025,
            0.050,
            0.075,
            0.100,
            0.200,
            0.300,
            0.400,
            0.500,
            0.600,
            0.700,
            0.800,
            0.900,
            1.0,
            2.0,
            5.0,
            10.0,
            20.0,
            30.0,
            60.0
        )
        .labelNames(
            "http_method",
            "request_path",
            "status_code"
        ).register(registry)
}