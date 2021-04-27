package dev.angerm.ag_server.redis

import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands

typealias SimpleRedis = RedisClusterAsyncCommands<String, String>

fun getFullRedis(sr: SimpleRedis): RedisAsyncCommands<String, String>? {
    if (sr is RedisAsyncCommands<String, String>) return sr
    return null
}

fun getFullClusterRedis(sr: SimpleRedis): RedisAdvancedClusterAsyncCommands<String, String>? {
    if (sr is RedisAdvancedClusterAsyncCommands<String, String>) return sr
    return null
}
