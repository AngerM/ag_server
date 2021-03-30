package dev.angerm.ag_server.redis

import io.lettuce.core.metrics.CommandLatencyRecorder
import io.lettuce.core.protocol.ProtocolKeyword
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Histogram
import java.net.SocketAddress

class PrometheusLettuceRecorder(registry: CollectorRegistry) : CommandLatencyRecorder {
    private val firstLatency = Histogram.build("lettuce.command.first_response_latency", "latency metrics for lettuce commands in nanoseconds")
        .exponentialBuckets(100_000.0, 2.0, 16)
        .labelNames(
            "remote",
            "command",
        ).register(registry)

    private val completionLatency = Histogram.build("lettuce.command.latency", "latency metrics for lettuce commands in nanoseconds")
        .exponentialBuckets(100_000.0, 2.0, 16)
        .labelNames(
            "remote",
            "command",
        ).register(registry)

    override fun recordCommandLatency(
        local: SocketAddress?,
        remote: SocketAddress?,
        commandType: ProtocolKeyword?,
        firstResponseLatency: Long,
        completionLatency: Long
    ) {
        this.firstLatency.labels(
            remote?.toString() ?: "UNKNOWN",
            commandType?.name() ?: "UNKNOWN"
        ).observe(firstResponseLatency.toDouble())

        this.completionLatency.labels(
            remote?.toString() ?: "UNKNOWN",
            commandType?.name() ?: "UNKNOWN"
        ).observe(completionLatency.toDouble())
    }
}