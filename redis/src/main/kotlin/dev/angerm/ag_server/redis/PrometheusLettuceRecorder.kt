package dev.angerm.ag_server.redis

import io.lettuce.core.metrics.CommandLatencyRecorder
import io.lettuce.core.protocol.ProtocolKeyword
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Histogram
import java.net.SocketAddress


class PrometheusLettuceRecorder(private val cluster: String, private val metrics: Metrics) : CommandLatencyRecorder {
    class Metrics(registry: CollectorRegistry) {
        val firstLatency = Histogram.build("lettuce_command_first_response_latency", "latency metrics for lettuce commands in nanoseconds")
            .exponentialBuckets(100_000.0, 2.0, 16)
            .labelNames(
                "cluster",
                "command",
            ).register(registry)

        val completionLatency = Histogram.build("lettuce_command_latency", "latency metrics for lettuce commands in nanoseconds")
            .exponentialBuckets(100_000.0, 2.0, 16)
            .labelNames(
                "cluster",
                "command",
            ).register(registry)
    }

    override fun recordCommandLatency(
        local: SocketAddress?,
        remote: SocketAddress?,
        commandType: ProtocolKeyword?,
        firstResponseLatency: Long,
        completionLatency: Long
    ) {
        metrics.firstLatency.labels(
            cluster,
            commandType?.name() ?: "UNKNOWN"
        ).observe(firstResponseLatency.toDouble())

        metrics.completionLatency.labels(
            cluster,
            commandType?.name() ?: "UNKNOWN"
        ).observe(completionLatency.toDouble())
    }
}