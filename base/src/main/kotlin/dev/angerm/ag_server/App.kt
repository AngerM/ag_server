package dev.angerm.ag_server

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Inject
import com.google.inject.Injector
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.ServerBuilder
import com.uchuhimo.konf.Config
import dev.angerm.ag_server.http.HttpHandler
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * The App interface is mainly here to allow Guice proxying during instantiation
 */
interface App {
    fun start(): CompletableFuture<*>
    fun stop(): CompletableFuture<*>
    fun runBlocking()
    fun port(): Int
    fun getInjector(): Injector?
    fun setInjector(injector: Injector)

    companion object {
        fun createInjector(vararg modules: AbstractModule): Injector {
            val env = Environment()
            return Guice.createInjector(
                env.getGuiceStage(),
                AgModule(env),
                *modules,
            )
        }
        /**
         * Convenience method to get an App instance from the injector
         * @param injector a Guice injector that has had AgModule added to it
         * @return an App instance to start the server from
         */
        fun getServer(injector: Injector): App {
            val app = injector.getInstance(App::class.java)
            app.setInjector(injector)
            return app
        }

        /**
         * Convenience method for writing unit tests by running a full server instance
         * on a random port.
         * <pre>{@code
         * @Test fun myTest() = App.testServer { server ->
         *   // Whatever you want to test here
         * }
         * }</pre>
         * @param modules Guice modules you want to be added to this server
         * @param f generally a lambda you that contains your actual test
         */
        fun testServer(vararg modules: AbstractModule, rawYamlConfig: String = "", f: suspend (App) -> Any) {
            val env = Environment()
            val injector = Guice.createInjector(
                env.getGuiceStage(),
                AgModule(env, registry = CollectorRegistry(), autoPort = true, rawYamlConfig = rawYamlConfig),
                *modules,
            )
            val server = getServer(injector)
            server.start()
            try {
                runBlocking {
                    f(server)
                }
            } finally {
                server.stop().join()
            }
        }
    }
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
    private val shutdownTimeoutSeconds: Long = config[BaseSpec.shutdownTimeoutSeconds]
    private val server: Server
    private val logger = KotlinLogging.logger {}
    private var appInjector: Injector? = null

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
        ).orTimeout(shutdownTimeoutSeconds, TimeUnit.SECONDS)
    }

    override fun runBlocking() {
        start().join()
    }

    override fun port(): Int =
        server.config().ports().first()?.localAddress()?.port ?: 0

    override fun getInjector(): Injector? = appInjector

    override fun setInjector(injector: Injector) {
        appInjector = injector
    }
}

fun main() {
    val injector = App.createInjector()
    val server = App.getServer(injector)
    server.runBlocking()
}