package dev.angerm

import com.linecorp.armeria.client.WebClient
import dev.angerm.ag_server.App
import kotlin.test.Test
import kotlin.test.assertEquals

class AppTest {
    @Test fun testAppDefault() = App.testServer { server ->
        val client = WebClient.of("http://localhost:${server.port()}")
        val agg = client.get("/").aggregate().join()
        assertEquals(200, agg.status().code())
    }

    @Test fun testProm() = App.testServer { server ->
        val client = WebClient.of("http://localhost:${server.port()}")
        val agg = client.get("/metrics").aggregate().join()
        assertEquals(200, agg.status().code())
        assert(agg.content().toStringUtf8().contains("jvm_info"))
    }
}