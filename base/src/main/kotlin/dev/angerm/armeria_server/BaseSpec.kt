package dev.angerm.armeria_server

import com.uchuhimo.konf.ConfigSpec

object BaseSpec : ConfigSpec() {
    val port by optional(8080)
    val numWorkerThreads by optional(64)
    val maxConnectionAgeMillis by optional(60000)
    val maxNumConnections by optional(500)
    val blockingTaskThreadPoolSize by optional(64)
}
