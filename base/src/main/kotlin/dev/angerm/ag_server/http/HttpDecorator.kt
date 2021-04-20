package dev.angerm.ag_server.http

import com.linecorp.armeria.server.DecoratingHttpServiceFunction
import com.linecorp.armeria.server.Route

interface HttpDecorator: DecoratingHttpServiceFunction {
    fun forRoute(): Route = Route.ofCatchAll()
}