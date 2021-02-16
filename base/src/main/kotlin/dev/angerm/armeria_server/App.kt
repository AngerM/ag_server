package dev.angerm.armeria_server

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Inject
import com.google.inject.Provides
import com.google.inject.multibindings.OptionalBinder
import com.google.inject.multibindings.ProvidesIntoSet
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.ServerBuilder
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.yaml
import dev.angerm.armeria_server.http_handler.HttpHandler
import dev.angerm.armeria_server.http_handler.PrometheusHandler
import dev.angerm.armeria_server.storage.DatabaseClientModule
import javax.annotation.Nullable

class App : AbstractModule() {
    private val sb = Server.builder()
    private val environment = System.getenv("ENVIRONMENT")?.toLowerCase() ?: "test"

    override fun configure() {
        bind(ServerBuilder::class.java).toInstance(sb)
    }

    @ProvidesIntoSet
    fun getSpec(): ConfigSpec {
        return BaseSpec
    }

    @ProvidesIntoSet
    fun getPromHttp(): HttpHandler {
        return PrometheusHandler()
    }

    class ArmeriaAddons {
        @Inject(optional = true) val addons = setOf<ArmeriaAddon>()
    }

    @Provides
    @Inject
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
    fun getConfig(
        specs: Set<ConfigSpec>
    ): Config {
        return Config {
            specs.forEach {
                this.addSpec(it)
            }
        }
            .from.yaml.file("base.yml", true)
            .from.json.file("base.json", true)
            .from.yaml.file("$environment.yml", true)
            .from.json.file("$environment.json", true)
            .from.systemProperties()
            .from.env()
    }
}

fun main() {
    val injector = Guice.createInjector(
        App(),
        DatabaseClientModule(),
    )
    val server = injector.getInstance(Server::class.java)
    server.start().join()
}
