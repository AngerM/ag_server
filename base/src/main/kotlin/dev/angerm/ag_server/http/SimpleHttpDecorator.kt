package dev.angerm.ag_server.http

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.logging.RequestLog
import com.linecorp.armeria.server.HttpService
import com.linecorp.armeria.server.Route
import com.linecorp.armeria.server.ServiceRequestContext

open class SimpleHttpDecorator {
    class Wrapper(
        private val route: Route? = null,
        private val simpleHttpDecorator: SimpleHttpDecorator,
    ) : HttpDecorator {
        override fun forRoute(): Route {
            return route ?: super.forRoute()
        }

        override fun serve(delegate: HttpService, ctx: ServiceRequestContext, req: HttpRequest): HttpResponse {
            simpleHttpDecorator.start(ctx, req)
            ctx.log().whenComplete().thenAccept { log ->
                simpleHttpDecorator.end(ctx, req, log)
            }
            return delegate.serve(ctx, req)
        }
    }
    open fun start(ctx: ServiceRequestContext, req: HttpRequest) {}
    open fun end(ctx: ServiceRequestContext, req: HttpRequest, log: RequestLog) {}
}