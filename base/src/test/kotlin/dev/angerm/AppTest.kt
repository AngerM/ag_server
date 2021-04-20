package dev.angerm

import com.google.inject.Guice
import com.linecorp.armeria.client.WebClient
import dev.angerm.ag_server.AgModule
import dev.angerm.ag_server.App
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class AppTest {
    fun withServer(f: suspend (App) -> Any) {
        val injector = Guice.createInjector(
            AgModule(registry = CollectorRegistry(), autoPort = true),
        )
        val server = AgModule.getServer(injector)
        server.start()
        try {
            runBlocking {
                f(server)
            }
        } finally {
            server.stop().join()
        }
    }
    @Test fun testAppDefault() = withServer { server ->
        val client = WebClient.of("http://localhost:${server.port()}")
        val agg = client.get("/").aggregate().join()
        assertEquals(200, agg.status().code())
    }

    @Test fun testProm() = withServer { server ->
        val client = WebClient.of("http://localhost:${server.port()}")
        val agg = client.get("/metrics").aggregate().join()
        assertEquals(200, agg.status().code())
        assert(agg.content().toStringUtf8().contains("jvm_info"))
    }
}