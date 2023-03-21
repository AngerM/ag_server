package dev.angerm.ag_server.http

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.logging.RequestLog
import com.linecorp.armeria.server.HttpService
import com.linecorp.armeria.server.Route
import com.linecorp.armeria.server.ServiceRequestContext

abstract class SimpleHttpDecorator {
    class Wrapper(
        private val route: Route? = null,
        private val factory: (HttpService, ServiceRequestContext, HttpRequest) -> SimpleHttpDecorator,
    ) : HttpDecorator {
        override fun forRoute(): Route {
            return route ?: super.forRoute()
        }

        override fun serve(delegate: HttpService, ctx: ServiceRequestContext, req: HttpRequest): HttpResponse {
            val decorator = factory(delegate, ctx, req)
            decorator.start()
            ctx.log().whenComplete().thenAccept { log ->
                decorator.end(log)
            }
            return delegate.serve(ctx, req)
        }
    }
    abstract fun start()
    abstract fun end(log: RequestLog)
}