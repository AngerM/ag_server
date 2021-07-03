package dev.angerm.ag_server.redis

import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands

data class SimpleRedis(
    val client: RedisClusterAsyncCommands<String, String>
) : RedisClusterAsyncCommands<String, String> by (client) {
    fun getFullRedis(): RedisAsyncCommands<String, String>? {
        if (this.client is RedisAsyncCommands<String, String>) return this.client
        return null
    }

    fun getFullClusterRedis(): RedisAdvancedClusterAsyncCommands<String, String>? {
        if (this.client is RedisAdvancedClusterAsyncCommands<String, String>) return this.client
        return null
    }
}