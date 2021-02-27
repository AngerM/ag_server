package dev.angerm.ag_server

import com.linecorp.armeria.server.ServerBuilder

interface ArmeriaAddon {
    fun apply(builder: ServerBuilder)
}