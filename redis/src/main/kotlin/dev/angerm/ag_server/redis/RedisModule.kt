package dev.angerm.ag_server.redis

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.multibindings.ProvidesIntoSet
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import io.lettuce.core.RedisClient
import io.lettuce.core.cluster.RedisClusterClient
import io.lettuce.core.metrics.CommandLatencyRecorder
import io.lettuce.core.resource.ClientResources
import io.prometheus.client.CollectorRegistry

data class RedisContainer(
    val redisClients: Map<String, RedisClient>,
    val clusterClients: Map<String, RedisClusterClient>
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

    @Provides
    @Inject
    @Singleton
    fun getRedis(
        recorder: CommandLatencyRecorder,
        conf: Config
    ): RedisContainer {
        val redisConfigs = conf[RedisSpec.redis]
        val redis = redisConfigs.filterValues {
            !it.isCluster
        }.map {
            val resources = ClientResources.builder()
                .apply {
                    this.commandLatencyRecorder(recorder)
                }.build()
            it.key to RedisClient.create(resources, it.value.uri)
        }.toMap()

        val cluster = redisConfigs.filterValues {
            it.isCluster
        }.map {
            val resources = ClientResources.builder()
                .apply {
                    this.commandLatencyRecorder(recorder)
                }.build()
            it.key to RedisClusterClient.create(resources, it.value.uri)
        }.toMap()
        return RedisContainer(
            redis,
            cluster
        )
    }
}