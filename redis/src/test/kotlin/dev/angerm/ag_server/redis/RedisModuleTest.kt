package dev.angerm.ag_server.redis

import dev.angerm.ag_server.App
import kotlinx.coroutines.future.await
import redis.embedded.RedisServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RedisModuleTest {
    @Test fun testRedisClient() = App.testServer(
        RedisModule(),
        rawYamlConfig = """
        redis:
            default:
                uri: "redis://localhost:6379"
        """.trimIndent()
    ) { server ->
        val redis = RedisServer()
        try {
            redis.start()
            val container = server.getInjector()?.getInstance(RedisContainer::class.java)
            val client = container?.redisClients?.get("default")
            assertNotNull(client)
            val ping = client.ping().await()
            assertEquals("PONG", ping)
        } finally {
            redis.stop()
        }
    }
}