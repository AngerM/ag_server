package dev.angerm.ag_server.grpc

import com.google.inject.Inject
import com.google.inject.name.Named
import com.linecorp.armeria.server.ServerBuilder
import com.linecorp.armeria.server.grpc.GrpcService
import dev.angerm.ag_server.ArmeriaAddon
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.ServerServiceDefinition
import io.grpc.protobuf.services.ProtoReflectionService
import me.dinowernli.grpc.prometheus.Configuration
import me.dinowernli.grpc.prometheus.MonitoringServerInterceptor

class GrpcBuilder : ArmeriaAddon {
    companion object {
        const val GLOBAL_INTERCEPTORS = "Global"
    }
    private val builder = GrpcService.builder()
    @Inject(optional = true) private val bindableServices: Set<ServerServiceDefinition> = setOf()
    @Inject(optional = true) @Named(GLOBAL_INTERCEPTORS) private val injectedInterceptors: Set<ServerInterceptor> = setOf()
    private val defaultInterceptors: MutableSet<ServerInterceptor> = mutableSetOf()

    init {
        builder.addService(ProtoReflectionService.newInstance())
        defaultInterceptors.addAll(injectedInterceptors)
        defaultInterceptors.add(MonitoringServerInterceptor.create(Configuration.allMetrics()))
    }

    private fun build(): GrpcService {
        bindableServices.forEach {
            builder.addService(
                ServerInterceptors.intercept(it, defaultInterceptors.toList())
            )
        }
        return builder.build()
    }

    override fun apply(builder: ServerBuilder) {
        builder.service(build())
    }
}