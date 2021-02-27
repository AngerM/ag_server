package dev.angerm.ag_server.redis

import com.uchuhimo.konf.ConfigSpec

object RedisSpec : ConfigSpec("") {
    data class RedisConfig(
        val isCluster: Boolean = false,
        val uri: String? = null
    )
    val redis by optional(mapOf<String, RedisConfig>())
}