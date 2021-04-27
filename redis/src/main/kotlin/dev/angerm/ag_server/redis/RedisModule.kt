package dev.angerm.ag_server.redis

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.multibindings.ProvidesIntoSet
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import io.lettuce.core.ClientOptions
import io.lettuce.core.ReadFrom
import io.lettuce.core.RedisClient
import io.lettuce.core.SocketOptions
import io.lettuce.core.TimeoutOptions
import io.lettuce.core.cluster.ClusterClientOptions
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions
import io.lettuce.core.cluster.RedisClusterClient
import io.lettuce.core.metrics.CommandLatencyRecorder
import io.lettuce.core.resource.ClientResources
import io.prometheus.client.CollectorRegistry
import java.time.Duration

data class RedisContainer(
    val redisClients: Map<String, SimpleRedis>,
)

class RedisModule : AbstractModule() {
    @ProvidesIntoSet
    fun getConfig(): ConfigSpec {
        return RedisSpec
    }

    @Provides
    @Singleton
    fun getCommandLatencyRecorder(
        collectorRegistry: CollectorRegistry
    ): CommandLatencyRecorder {
        return PrometheusLettuceRecorder(collectorRegistry)
    }

    private fun getClientOptions(): ClientOptions {
        val socketOptions = SocketOptions.builder()
            .connectTimeout(Duration.ofSeconds(5))
            .keepAlive(true)
            .build()
        val timeoutOptions = TimeoutOptions.builder()
            .timeoutCommands(true)
            .fixedTimeout(Duration.ofMillis(500))
            .build()

        return ClientOptions.builder()
            .timeoutOptions(timeoutOptions)
            .socketOptions(socketOptions)
            .cancelCommandsOnReconnectFailure(true)
            .autoReconnect(true)
            .build()
    }

    private fun getClusterOptions(): ClusterClientOptions {
        val clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
            .enableAllAdaptiveRefreshTriggers()
            .refreshTriggersReconnectAttempts(3)
            .enablePeriodicRefresh(Duration.ofSeconds(15))
            .closeStaleConnections(true)
            .dynamicRefreshSources(true)
            .build()
        val clientOptions = getClientOptions()

        return ClusterClientOptions.builder(clientOptions)
            .topologyRefreshOptions(clusterTopologyRefreshOptions)
            .build()
    }

    @Provides
    @Inject
    @Singleton
    fun getRedis(
        recorder: CommandLatencyRecorder,
        conf: Config
    ): RedisContainer {
        val redisConfigs = conf[RedisSpec.redis]
        val redis = redisConfigs.map {
            val resources = ClientResources.builder()
                .apply {
                    this.commandLatencyRecorder(recorder)
                }.build()
            if (it.value.isCluster) {
                val options = getClusterOptions()
                val client = RedisClusterClient.create(resources, it.value.uri)
                client.setOptions(options)
                it.key to client.connect().apply {
                    this.readFrom = ReadFrom.REPLICA_PREFERRED
                }.async()
            } else {
                val options = getClientOptions()
                val client = RedisClient.create(resources, it.value.uri)
                client.options = options
                it.key to client.connect().async()
            }
        }.toMap()
        return RedisContainer(
            redis,
        )
    }
}