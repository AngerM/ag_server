package dev.angerm.ag_server

import com.google.inject.Guice
import com.google.inject.Inject
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.ServerBuilder
import com.uchuhimo.konf.Config
import dev.angerm.ag_server.http.HttpHandler
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

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
    config: Config,
    builder: ServerBuilder,
    handlers: Set<HttpHandler>,
    decorators: AgModule.HttpDecorators,
    private val addons: AgModule.ArmeriaAddons
) : App {
    private val shutdownTimeoutSecods: Long = config[BaseSpec.shutdownTimeoutSeconds]
    private val server: Server
    private val logger = Logger.getLogger(this::class.java.canonicalName)
    init {
        logger.info("App creation started")
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
        val shutdownHook = {
            this.stop().join()
        }
        Runtime.getRuntime().addShutdownHook(
            Thread {
                try {
                    shutdownHook()
                } catch (_: Exception) {
                    // do nothing
                } finally {
                    logger.info("Shutdown complete!")
                }
            }
        )
        logger.info("Server build complete. Ready for use")
    }
    override fun start(): CompletableFuture<*> {
        logger.info("Server starting!")
        addons.addons.forEach {
            it.start()
        }
        return server.start()
    }

    override fun stop(): CompletableFuture<*> {
        logger.info("Server shutdown started")
        val futures = addons.addons.map {
            it.stop()
        }
        return CompletableFuture.allOf(
            *futures.toTypedArray(),
            server.stop()
        ).orTimeout(shutdownTimeoutSecods, TimeUnit.SECONDS)
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