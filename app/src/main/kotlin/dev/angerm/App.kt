package dev.angerm

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.ServerBuilder

class App: AbstractModule() {
    private val sb = Server.builder()
    override fun configure() {
       bind(ServerBuilder::class.java).toInstance(sb)
    }
}

fun main() {
    val injector = Guice.createInjector(
        App()
    )
    val sb = injector.getInstance(ServerBuilder::class.java)
    val server = sb.build()
    server.start().join()
}
