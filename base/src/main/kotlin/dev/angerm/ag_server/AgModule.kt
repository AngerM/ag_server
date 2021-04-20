package dev.angerm.ag_server

import com.google.inject.*
import com.google.inject.multibindings.Multibinder
import com.google.inject.multibindings.ProvidesIntoSet
import com.linecorp.armeria.common.SessionProtocol
import com.linecorp.armeria.common.util.EventLoopGroups
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.ServerBuilder
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.toml
import com.uchuhimo.konf.source.yaml
import dev.angerm.ag_server.http.DefaultHandler
import dev.angerm.ag_server.http.HttpDecorator
import dev.angerm.ag_server.http.HttpHandler
import dev.angerm.ag_server.http.HttpMetricDecorator
import dev.angerm.ag_server.http.PrometheusHandler
import dev.angerm.ag_server.http.SimpleHttpDecorator
import io.netty.channel.ChannelOption
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import java.net.ServerSocket
import java.time.Duration
import java.util.concurrent.Executors

/**
 * The base module for the Ag Wrapper, provides functionality for serving HTTP requests
 *
 * @param defaultHandler the default handler
 * @param registry the collector registry, defaults to the prometheus defaultRegistry
 * @param autoPort tells AgModule to ignore the config and bind to any available port
 */
class AgModule(
    private val defaultHandler: HttpHandler = DefaultHandler(),
    private val registry: CollectorRegistry = CollectorRegistry.defaultRegistry,
    private val autoPort: Boolean = false,
) : AbstractModule() {
    companion object {
        /**
         * Convenience method to get an App instance from the injector
         * @param injector a Guice injector that has had AgModule added to it
         * @return an App instance to start the server from
         */
        fun getServer(injector: Injector): App = injector.getInstance(App::class.java)
    }
    private val environment = System.getenv("ENVIRONMENT")?.toLowerCase() ?: "test"

    override fun configure() {
        if (registry != CollectorRegistry.defaultRegistry) {
            DefaultExports.register(registry)
        }
        bind(CollectorRegistry::class.java).toInstance(registry)
        Multibinder.newSetBinder(binder(), HttpHandler::class.java).addBinding().toInstance(defaultHandler)
    }

    /**
     * This is an example of how to inject other ConfigSpec's into the Konf parser.
     * Add your own via a similar
     * <pre>{@code
     * @ProvidesIntoSet
     * fun getMySpec(): ConfigSpec {
     *   return MySpec
     * }
     * }</pre>
     * @return a config spec for Konf to parse
     */
    @ProvidesIntoSet
    fun getSpec(): ConfigSpec {
        return BaseSpec
    }

    /**
     * This is an example of how to inject other HttpHandlers into this server
     * Add your own via a similar
     * <pre>{@code
     * @ProvidesIntoSet
     * fun getMyHandler(): HttpHandler {
     *   return MyHandler()
     * }
     * }</pre>
     * @return a HttpHandler to add to the server
     */
    @ProvidesIntoSet
    fun getPromHttp(registry: CollectorRegistry): HttpHandler {
        return PrometheusHandler(registry)
    }

    /**
     * This is a simple wrapper class to work around some Guice stuff
     * You shouldn't have to deal with it
     */
    class ArmeriaAddons {
        @Inject(optional = true) val addons = setOf<ArmeriaAddon>()
    }

    /**
     * This is a simple wrapper class to work around some Guice stuff
     * You shouldn't have to deal with it
     */
    class HttpDecorators {
        @Inject(optional = true) val decorators = setOf<List<HttpDecorator>>()
    }

    @Provides
    @Inject
    @Singleton
    fun getBuilder(
        conf: Config
    ): ServerBuilder {
        val sb = Server.builder()
        if (!autoPort) {
            sb.port(conf[BaseSpec.port], SessionProtocol.HTTP)
        } else {
            val port = ServerSocket(0)
            val localPort = port.localPort
            port.close()
            sb.port(localPort, SessionProtocol.HTTP)
        }
        sb.workerGroup(EventLoopGroups.newEventLoopGroup(conf[BaseSpec.numWorkerThreads], "worker_", true), true)
        sb.maxConnectionAge(Duration.ofSeconds(conf[BaseSpec.maxConnectionAgeSeconds]))
        sb.maxNumConnections(conf[BaseSpec.maxNumConnections])
        sb.blockingTaskExecutor(Executors.newScheduledThreadPool(conf[BaseSpec.blockingTaskThreadPoolSize]), true)
        sb.requestTimeout(Duration.ofSeconds(conf[BaseSpec.requestTimeoutSeconds]))
        sb.channelOption(ChannelOption.SO_BACKLOG, conf[BaseSpec.socketBacklog])
        sb.channelOption(ChannelOption.SO_REUSEADDR, conf[BaseSpec.reuseAddr])
        sb.childChannelOption(ChannelOption.SO_SNDBUF, conf[BaseSpec.sndBuf])
        sb.childChannelOption(ChannelOption.SO_RCVBUF, conf[BaseSpec.rcvBuf])
        return sb
    }

    @Provides
    @Inject
    @Singleton
    fun getServer(app: AppImpl): App = app

    @Provides
    @Inject
    @Singleton
    fun getConfig(
        specs: Set<ConfigSpec>
    ): Config {
        return Config {
            specs.forEach {
                this.addSpec(it)
            }
        }
            .from.yaml.resource("base.yml", true)
            .from.json.resource("base.json", true)
            .from.toml.resource("base.toml", true)
            .from.yaml.resource("$environment.yml", true)
            .from.json.resource("$environment.json", true)
            .from.toml.resource("$environment.toml", true)
            .from.systemProperties()
            .from.env()
    }

    @ProvidesIntoSet
    @Inject
    fun getDefaultDecorators(
        metrics: Metrics
    ):List<HttpDecorator> {
        return listOf<HttpDecorator>(
            SimpleHttpDecorator.Wrapper { _, ctx, _ ->
                HttpMetricDecorator(ctx, metrics)
            }
        )
    }
}