package dev.angerm.ag_server.redis

import com.uchuhimo.konf.ConfigSpec

object RedisSpec : ConfigSpec("") {
    data class RedisConfig(
        val isCluster: Boolean = false,
        val uri: String? = null,
        val connectTimeoutMillis: Long = 5_000,
        val fixedTimeoutMillis: Long = 500,
        val periodicRefreshTimerMillis: Long = 15_000
    )
    val redis by optional(mapOf<String, RedisConfig>())
}