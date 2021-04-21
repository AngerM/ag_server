package dev.angerm.ag_server

import com.google.inject.Guice
import com.google.inject.Inject
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.ServerBuilder
import dev.angerm.ag_server.http.HttpHandler
import java.util.concurrent.CompletableFuture

/**
 * The App interface is mainly here to allow Guice proxying during instantiation
 */
interface App {
    fun start(): CompletableFuture<*>
    fun stop(): CompletableFuture<*>
    fun runBlocking()
    fun port(): Int
}

/**
 * Actual wrapper class for the Armeria server and its addons
 *
 * @param builder an Armeria ServerBuilder class
 * @param handlers the HttpHandlers to register
 * @param decorators decorators to add to this server (middleware)
 * @param addons other addons you would like the wrapper class to control
 */
class AppImpl @Inject constructor(
    builder: ServerBuilder,
    handlers: Set<HttpHandler>,
    decorators: AgModule.HttpDecorators,
    private val addons: AgModule.ArmeriaAddons
) : App {
    private val server: Server
    init {
        decorators.decorators.forEach {
            orderedDecoratorList ->
            orderedDecoratorList.forEach {
                builder.decorator(it.forRoute(), it)
            }
        }
        handlers.forEach {
            builder.annotatedService(it.pathPrefix, it)
        }
        addons.addons.forEach {
            it.apply(builder)
        }
        server = builder.build()
    }
    override fun start(): CompletableFuture<*> {
        addons.addons.forEach {
            it.start()
        }
        return server.start()
    }

    override fun stop(): CompletableFuture<*> {
        val futures = addons.addons.map {
            it.stop()
        }
        return CompletableFuture.allOf(
            *futures.toTypedArray(),
            server.stop()
        )
    }

    override fun runBlocking() {
        start().join()
    }

    override fun port(): Int =
        server.config().ports().first()?.localAddress()?.port ?: 0
}

fun main() {
    val injector = Guice.createInjector(
        AgModule(),
    )
    val server = AgModule.getServer(injector)
    server.runBlocking()
}