package dev.angerm.ag_server.database

import com.uchuhimo.konf.ConfigSpec

class DatabaseSpec: ConfigSpec("") {
    data class DatabaseConfig(
        val dbType: String,
        val hostname: String,
        val username: String = "",
        val passwordEnvVar: String = "",
    )
    val database by optional(mapOf<String, DatabaseConfig>())
}