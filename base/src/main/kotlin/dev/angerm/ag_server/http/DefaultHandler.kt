package dev.angerm.ag_server.http

import com.linecorp.armeria.server.annotation.Get

class DefaultHandler : HttpHandler {
    @Get("/")
    fun get(): String {
        return "Hello"
    }

    override val pathPrefix: String
        get() = "/"
}