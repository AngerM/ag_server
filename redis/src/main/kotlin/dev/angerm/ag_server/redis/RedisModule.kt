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

class RedisModule : AbstractModule() {
    @ProvidesIntoSet
    fun getConfig(): ConfigSpec {
        return RedisSpec
    }

    @Provides
    @Inject
    @Singleton
    fun getRedis(
        conf: Config
    ): Map<String, RedisClient> {
        val redisConfigs = conf[RedisSpec.redis]
        return redisConfigs.filterValues {
            !it.isCluster
        }.map {
            it.key to RedisClient.create(it.value.uri)
        }.toMap()
    }

    @Provides
    @Inject
    @Singleton
    fun getRedisCluster(
        conf: Config
    ): Map<String, RedisClusterClient> {
        val redisConfigs = conf[RedisSpec.redis]
        return redisConfigs.filterValues {
            it.isCluster
        }.map {
            it.key to RedisClusterClient.create(it.value.uri)
        }.toMap()
    }
}