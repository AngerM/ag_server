package dev.angerm.ag_server.grpc

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.multibindings.ProvidesIntoSet
import com.uchuhimo.konf.ConfigSpec
import dev.angerm.ag_server.ArmeriaAddon
import dev.angerm.ag_server.grpc.services.HealthService

class GrpcModule(
    private val healthService: HealthService = HealthService(),
) : AbstractModule() {
    override fun configure() {
        bind(HealthService::class.java).toInstance(healthService)
    }

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