package dev.angerm.ag_server.http

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.logging.RequestLog
import com.linecorp.armeria.server.ServiceRequestContext
import dev.angerm.ag_server.Metrics
import dev.angerm.ag_server.utils.toSecondsAsDouble
import java.time.Duration

class HttpMetricDecorator(
    private val metrics: Metrics,
) : SimpleHttpDecorator() {

    override fun end(ctx: ServiceRequestContext, req: HttpRequest, log: RequestLog) {
        metrics.httpCounter.labels(
            ctx.method().name,
            ctx.config().route().patternString(),
            log.responseHeaders().status().codeAsText(),
        ).inc()
        val responseTime = Duration.ofNanos(log.responseDurationNanos())
        metrics.httpLatency.labels(
            ctx.method().name,
            ctx.config().route().patternString(),
            log.responseHeaders().status().codeAsText(),
        ).observe(responseTime.toSecondsAsDouble())
    }
}