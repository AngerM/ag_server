package dev.angerm.ag_server.database

import com.uchuhimo.konf.ConfigSpec

object DatabaseSpec : ConfigSpec("") {
    data class DatabaseConfig(
        val driver: String,
        val protocol: String = "",
        val hostname: String = "",
        val port: Int = 0,
        val database: String,
        val username: String = "",
        val passwordEnvVar: String = "",
        val ssl: Boolean = false,
        val poolInitialSize: Int = 10,
        val poolMaxSize: Int = 10,
        val otherOptions: Map<String, Any> = mapOf()
    )
    val database by optional(mapOf<String, DatabaseConfig>())
}