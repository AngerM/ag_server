package dev.angerm.ag_server.grpc

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.multibindings.ProvidesIntoSet
import com.uchuhimo.konf.ConfigSpec
import dev.angerm.ag_server.ArmeriaAddon

class GrpcModule : AbstractModule() {
    @ProvidesIntoSet
    fun getConf(): ConfigSpec {
        return GrpcSpec
    }

    @ProvidesIntoSet
    @Inject
    fun getBuilder(
        impl: GrpcBuilder
    ): ArmeriaAddon {
        return impl
    }
}