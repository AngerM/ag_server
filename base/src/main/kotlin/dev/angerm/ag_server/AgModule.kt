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
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.hocon
import com.uchuhimo.konf.source.toml
import com.uchuhimo.konf.source.yaml
import dev.angerm.ag_server.http.DefaultHandler
import dev.angerm.ag_server.http.HttpDecorator
import dev.angerm.ag_server.http.HttpHandler
import dev.angerm.ag_server.http.PrometheusHandler
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
    private val environment: Environment,
    private val defaultHandler: HttpHandler = DefaultHandler(),
    private val registry: CollectorRegistry = CollectorRegistry.defaultRegistry,
    private val autoPort: Boolean = false,
    private val rawYamlConfig: String = "",
) : AbstractModule() {

    override fun configure() {
        if (registry != CollectorRegistry.defaultRegistry) {
            DefaultExports.register(registry)
        }
        bind(CollectorRegistry::class.java).toInstance(registry)
        Multibinder.newSetBinder(binder(), HttpHandler::class.java).addBinding().toInstance(defaultHandler)
        bind(Environment::class.java).toInstance(environment)
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
        config: Config
    ): ServerBuilder {
        val sb = Server.builder()
        if (!autoPort) {
            sb.port(config[BaseSpec.port], SessionProtocol.HTTP)
        } else {
            val port = ServerSocket(0)
            val localPort = port.localPort
            port.close()
            sb.port(localPort, SessionProtocol.HTTP)
        }
        sb.workerGroup(EventLoopGroups.newEventLoopGroup(config[BaseSpec.numWorkerThreads], "worker_", true), true)
        sb.maxConnectionAge(Duration.ofSeconds(config[BaseSpec.maxConnectionAgeSeconds]))
        sb.maxNumConnections(config[BaseSpec.maxNumConnections])
        sb.http1MaxHeaderSize(config[BaseSpec.http1MaxHeaderSize])
        sb.http2MaxHeaderListSize(config[BaseSpec.http2MaxHeaderListSize])
        sb.blockingTaskExecutor(Executors.newScheduledThreadPool(config[BaseSpec.blockingTaskThreadPoolSize]), true)
        sb.requestTimeout(Duration.ofSeconds(config[BaseSpec.requestTimeoutSeconds]))
        sb.channelOption(ChannelOption.SO_BACKLOG, config[BaseSpec.socketBacklog])
        sb.channelOption(ChannelOption.SO_REUSEADDR, config[BaseSpec.reuseAddr])
        sb.childChannelOption(ChannelOption.SO_SNDBUF, config[BaseSpec.sndBuf])
        sb.childChannelOption(ChannelOption.SO_RCVBUF, config[BaseSpec.rcvBuf])
        sb.childChannelOption(ChannelOption.TCP_FASTOPEN_CONNECT, config[BaseSpec.fastOpen])
        sb.childChannelOption(ChannelOption.TCP_NODELAY, config[BaseSpec.noDelay])
        if (environment.stage != Environment.Stage.Test) {
            sb.gracefulShutdownTimeout(
                Duration.ofSeconds(config[BaseSpec.gracefulShutdownTimeSeconds]),
                Duration.ofSeconds(config[BaseSpec.shutdownTimeoutSeconds]),
            )
        }
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
        specs: Set<ConfigSpec>,
    ): Config {
        val combinedSource = listOf("", "${environment.serviceName}/").map {
            prefix ->
            listOf("base", environment.stage.envVar).map {
                filename ->
                Source.from.yaml.resource("${prefix}$filename.yml", true) +
                    Source.from.json.resource("${prefix}$filename.json", true) +
                    Source.from.toml.resource("${prefix}$filename.toml", true) +
                    Source.from.hocon.resource("${prefix}$filename.hocon", true)
            }
        }.flatten().reduce {
            sum, item ->
            sum + item
        }
        return Config {
            specs.forEach {
                this.addSpec(it)
            }
        }.withSource(
            Source.from.yaml.string(rawYamlConfig) +
                combinedSource +
                Source.from.systemProperties() +
                Source.from.env()
        ).validateRequired()
    }
}