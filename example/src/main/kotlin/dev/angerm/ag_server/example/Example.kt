package dev.angerm.ag_server.example

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.multibindings.ProvidesIntoSet
import com.linecorp.armeria.server.annotation.Get
import com.linecorp.armeria.server.annotation.Param
import dev.angerm.ag_server.AgModule
import dev.angerm.ag_server.grpc.GrpcModule
import dev.angerm.ag_server.http_handler.HttpHandler
import dev.angerm.ag_server.redis.RedisContainer
import dev.angerm.ag_server.redis.RedisModule
import io.lettuce.core.RedisClient
import kotlinx.coroutines.future.await

class RedisHandler(redis: Map<String, RedisClient>) : HttpHandler {
    override val pathPrefix: String
        get() = "/redis"
    private val connection = redis["default"]?.connect()?.async()

    @Get("/:key")
    suspend fun get(@Param("key") key: String): String {
        return connection?.get(key)?.await() ?: "no key"
    }
}

class ExampleModule : AbstractModule() {
    @ProvidesIntoSet
    fun getRedisHandler(
        redis: RedisContainer
    ): HttpHandler {
        return RedisHandler(redis.redisClients)
    }
}

fun main() {
    val injector = Guice.createInjector(
        AgModule(),
        GrpcModule(),
        RedisModule(),
        ExampleModule(),
    )
    val server = AgModule.getServer(injector)
    server.runBlocking()
}