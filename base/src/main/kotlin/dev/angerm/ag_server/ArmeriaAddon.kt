package dev.angerm.ag_server

import com.linecorp.armeria.server.ServerBuilder
import java.util.concurrent.CompletableFuture

interface ArmeriaAddon {
    fun apply(builder: ServerBuilder)
    fun start()
    fun stop(): CompletableFuture<*>
}