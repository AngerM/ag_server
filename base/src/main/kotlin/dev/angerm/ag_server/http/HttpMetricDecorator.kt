package dev.angerm.ag_server.http

import com.linecorp.armeria.common.logging.RequestLog
import com.linecorp.armeria.server.ServiceRequestContext
import dev.angerm.ag_server.Metrics
import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import java.time.Duration

class HttpMetricDecorator(private val ctx: ServiceRequestContext, private val metrics: Metrics): SimpleHttpDecorator() {

    override fun start() {
    }

    override fun end(log: RequestLog) {
        metrics.httpCounter.labels(
            ctx.method().name,
            ctx.config().route().patternString(),
            log.responseHeaders().status().codeAsText()
        ).inc()
        metrics.httpLatency.labels(
            ctx.method().name,
            ctx.config().route().patternString(),
            log.responseHeaders().status().codeAsText()
        ).observe(Duration.ofNanos(log.responseDurationNanos()).toMillis().toDouble())
    }

}