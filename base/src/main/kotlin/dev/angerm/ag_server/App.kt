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
import dev.angerm.ag_server.http_handler.DefaultHandler
import dev.angerm.ag_server.http_handler.HttpHandler
import dev.angerm.ag_server.http_handler.PrometheusHandler
import io.netty.channel.ChannelOption
import io.prometheus.client.CollectorRegistry
import java.time.Duration
import java.util.concurrent.Executors

class App(
    private val defaultHandler: HttpHandler = DefaultHandler(),
    private val registry: CollectorRegistry = CollectorRegistry.defaultRegistry
) : AbstractModule() {
    private val environment = System.getenv("ENVIRONMENT")?.toLowerCase() ?: "test"

   override fun configure() {
      bind(CollectorRegistry::class.java).toInstance(registry)
       Multibinder.newSetBinder(binder(), HttpHandler::class.java).addBinding().toInstance(defaultHandler)
   }

    @ProvidesIntoSet
    fun getSpec(): ConfigSpec {
        return BaseSpec
    }

    @ProvidesIntoSet
    fun getPromHttp(registry: CollectorRegistry): HttpHandler {
        return PrometheusHandler(registry)
    }

    // Wrapper class for the optional injection in case there are none
    class ArmeriaAddons {
        @Inject(optional = true) val addons = setOf<ArmeriaAddon>()
    }

    @Provides
    @Inject
    @Singleton
    fun getBuilder(
        conf: Config
    ): ServerBuilder {
        val sb = Server.builder()
        sb.port(conf[BaseSpec.port], SessionProtocol.HTTP)
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
    fun getServer(
        builder: ServerBuilder,
        handlers: Set<HttpHandler>,
        addons: ArmeriaAddons,
    ): Server {
        handlers.forEach {
            builder.annotatedService(it.pathPrefix, it)
        }
        addons.addons.forEach {
            it.apply(builder)
        }
        return builder.build()
    }

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
}

fun main() {
    val injector = Guice.createInjector(
        App(),
    )
    val server = injector.getInstance(Server::class.java)
    server.start().join()
}