package dev.angerm.ag_server

import com.linecorp.armeria.server.ServerBuilder
import java.util.concurrent.CompletableFuture

/**
 * ArmeriaAddon is an interface you can use to have an addon be controlled by the App wrapper.
 * These includes start and stop callbacks and a hook for you to access the ServerBuilder
 */
interface ArmeriaAddon {
    /**
     * An apply function for ArmeriaAddons to modify the Server being constructed.
     *
     * @param builder the ServerBuilder being used to setup this armeria server
     */
    fun apply(builder: ServerBuilder) {}

    /**
     * start() will be called when during server bootup, BEFORE the armeria server has been started
     */
    fun start() {}

    /**
     * stop() will be called as the server is being shutdown
     * Return a CompletableFuture that will be join()'d with a timeout to allow for graceful shutdown
     *
     * @return a CompletableFuture that represents the state of this addon's shutdown
     */
    fun stop(): CompletableFuture<*> = CompletableFuture.completedFuture(true)
}