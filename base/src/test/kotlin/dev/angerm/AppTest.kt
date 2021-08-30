package dev.angerm

import com.linecorp.armeria.common.HttpHeaderNames
import com.linecorp.armeria.common.HttpMethod
import com.linecorp.armeria.common.HttpRequest
import dev.angerm.ag_server.App
import dev.angerm.ag_server.BaseSpec
import io.prometheus.client.exporter.common.TextFormat
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AppTest {
    @Test fun testAppDefault() = App.testServer { server ->
        val client = server.getHttpClient()
        val agg = client.get("/").aggregate().join()
        assertEquals(200, agg.status().code())
    }

    @Test fun testProm() = App.testServer { server ->
        val client = server.getHttpClient()
        client.get("/").aggregate().join()
        val agg = client.get("/metrics").aggregate().join()
        assertEquals(200, agg.status().code())
        assert(agg.content().toStringUtf8().contains("http_request_total"))
        assertEquals(TextFormat.CONTENT_TYPE_004, agg.headers().get(HttpHeaderNames.CONTENT_TYPE))

        val agg2 = client.execute(
            HttpRequest.builder().apply {
                this.method(HttpMethod.GET)
                this.path("/metrics")
                this.header(HttpHeaderNames.ACCEPT, TextFormat.CONTENT_TYPE_OPENMETRICS_100)
            }.build()
        ).aggregate().join()
        assertEquals(200, agg2.status().code())
        assert(agg2.content().toStringUtf8().contains("http_request_total"))
        assertEquals(TextFormat.CONTENT_TYPE_OPENMETRICS_100, agg2.headers().get(HttpHeaderNames.CONTENT_TYPE))
    }

    @Test fun testConfigResolution() = App.testServer { server ->
        val c = server.getConfig()
        assertEquals(2000, c[BaseSpec.port])
    }
}