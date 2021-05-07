package dev.angerm.ag_server.example

import com.google.inject.AbstractModule
import com.google.inject.multibindings.ProvidesIntoSet
import com.linecorp.armeria.common.logging.RequestLog
import com.linecorp.armeria.server.ServiceRequestContext
import com.linecorp.armeria.server.annotation.Get
import com.linecorp.armeria.server.annotation.Param
import com.linecorp.armeria.server.annotation.Post
import dev.angerm.ag_server.App
import dev.angerm.ag_server.grpc.GrpcModule
import dev.angerm.ag_server.http.HttpDecorator
import dev.angerm.ag_server.http.HttpHandler
import dev.angerm.ag_server.http.SimpleHttpDecorator
import dev.angerm.ag_server.redis.RedisContainer
import dev.angerm.ag_server.redis.RedisModule
import dev.angerm.ag_server.redis.SimpleRedis
import kotlinx.coroutines.future.await
import mu.KotlinLogging

class RedisHandler(private val connection: SimpleRedis) : HttpHandler {
    override val pathPrefix: String
        get() = "/redis"

    @Get("/:key")
    suspend fun get(@Param("key") key: String): String {
        return connection.get(key).await() ?: "Does not exist"
    }

    @Post("/:key")
    suspend fun post(@Param("key") key: String, body: String): String {
        return connection.set(key, body).await()
    }
}

class LoggingDecorator(ctx: ServiceRequestContext) : SimpleHttpDecorator() {
    private val logger = KotlinLogging.logger {}
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
        return RedisHandler(redis.redisClients["default"]!!)
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
    val injector = App.createInjector(
        GrpcModule(),
        RedisModule(),
        ExampleModule(),
    )
    val server = App.getServer(injector)
    server.runBlocking()
}