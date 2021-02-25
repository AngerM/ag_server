package dev.angerm.ag_server.redis

import com.uchuhimo.konf.ConfigSpec

object RedisSpec : ConfigSpec() {
    val uri by optional<String?>(null)
}
