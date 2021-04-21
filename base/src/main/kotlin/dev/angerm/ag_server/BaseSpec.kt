package dev.angerm.ag_server

import com.uchuhimo.konf.ConfigSpec

object BaseSpec : ConfigSpec() {
    val port by optional(50000)
    val numWorkerThreads by optional(64)
    val maxConnectionAgeSeconds by optional(300L)
    val maxNumConnections by optional(Integer.MAX_VALUE)
    val blockingTaskThreadPoolSize by optional(64)
    val requestTimeoutSeconds by optional(10L)
    val socketBacklog by optional(256)
    val reuseAddr by optional(true)
    val sndBuf by optional(1024 * 1024)
    val rcvBuf by optional(1024 * 1024)
    val gracefulShutdownTimeSeconds by optional(15L)
    val shutdownTimeoutSeconds by optional(20L)
}