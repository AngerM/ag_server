package dev.angerm.ag_server.example

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.multibindings.ProvidesIntoSet
import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.logging.RequestLog
import com.linecorp.armeria.server.ServiceRequestContext
import com.linecorp.armeria.server.annotation.Get
import com.linecorp.armeria.server.annotation.Param
import com.linecorp.armeria.server.annotation.Post
import dev.angerm.ag_server.AgModule
import dev.angerm.ag_server.grpc.GrpcModule
import dev.angerm.ag_server.http.HttpDecorator
import dev.angerm.ag_server.http.HttpHandler
import dev.angerm.ag_server.http.SimpleHttpDecorator
import dev.angerm.ag_server.redis.RedisContainer
import dev.angerm.ag_server.redis.RedisModule
import io.lettuce.core.RedisClient
import kotlinx.coroutines.future.await
import java.util.logging.Logger

class RedisHandler(redis: Map<String, RedisClient>) : HttpHandler {
    override val pathPrefix: String
        get() = "/redis"
    private val connection = redis["default"]?.connect()?.async()

    @Get("/:key")
    suspend fun get(@Param("key") key: String): String {
        return connection?.get(key)?.await() ?: "no key"
    }

    @Post("/:key")
    suspend fun post(@Param("key") key: String, body: String): String {
        return connection?.set(key, body)?.await() ?: "failure"
    }
}

class LoggingDecorator(ctx: ServiceRequestContext) : SimpleHttpDecorator() {
    private val logger = Logger.getLogger(this::class.java.canonicalName)
    private val pathPattern = ctx.config().route().patternString()
    override fun start() {
        logger.info("Request started for $pathPattern")
    }

    override fun end(log: RequestLog) {
        logger.info("Request ended for $pathPattern with ${log.responseHeaders().status().codeAsText()}")
    }
}

class ExampleModule : AbstractModule() {
    @ProvidesIntoSet
    fun getRedisHandler(
        redis: RedisContainer
    ): HttpHandler {
        return RedisHandler(redis.redisClients)
    }

    @ProvidesIntoSet
    fun addLoggingMiddleWare(): List<HttpDecorator> {
        return listOf<HttpDecorator>(
            SimpleHttpDecorator.Wrapper {
                _, ctx, _ ->
                LoggingDecorator(ctx)
            }
        )
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