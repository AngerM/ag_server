package dev.angerm.ag_server.redis

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Provides
import com.google.inject.multibindings.ProvidesIntoSet
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import io.lettuce.core.RedisClient

class RedisModule : AbstractModule() {
    @ProvidesIntoSet
    fun getConfig(): ConfigSpec {
        return RedisSpec
    }

    @Provides
    @Inject
    fun getRedisCluster(
        conf: Config
    ): RedisClient {
        return RedisClient.create(conf[RedisSpec.uri])
    }
}
