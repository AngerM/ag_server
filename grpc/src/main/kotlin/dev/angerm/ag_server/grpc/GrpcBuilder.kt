package dev.angerm.ag_server.grpc

import com.google.inject.Inject
import com.google.inject.name.Named
import com.linecorp.armeria.server.ServerBuilder
import com.linecorp.armeria.server.grpc.GrpcService
import com.uchuhimo.konf.Config
import dev.angerm.ag_server.ArmeriaAddon
import dev.angerm.ag_server.grpc.metrics.Configuration
import dev.angerm.ag_server.grpc.metrics.MonitoringServerInterceptor
import dev.angerm.ag_server.grpc.services.HealthService
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.ServerServiceDefinition
import io.grpc.protobuf.services.ProtoReflectionService
import io.prometheus.client.CollectorRegistry
import java.util.concurrent.CompletableFuture

class GrpcBuilder @Inject constructor(private val healthService: HealthService, conf: Config, collectorRegistry: CollectorRegistry) : ArmeriaAddon {
    companion object {
        const val GLOBAL_INTERCEPTORS = "Global"
    }
    private val builder = GrpcService.builder()
    @Inject(optional = true) private val bindableServices: Set<ServerServiceDefinition> = setOf()
    @Inject(optional = true) @Named(GLOBAL_INTERCEPTORS) private val injectedInterceptors: Set<ServerInterceptor> = setOf()
    private val defaultInterceptors: MutableSet<ServerInterceptor> = mutableSetOf()

    init {
        builder.addService(ProtoReflectionService.newInstance())
        builder.addService(healthService.bindService())
        builder.setMaxInboundMessageSizeBytes(conf[GrpcSpec.maxInboundMessageSizeBytes])
        builder.setMaxOutboundMessageSizeBytes(conf[GrpcSpec.maxOutboundMessageSizeBytes])
        defaultInterceptors.addAll(injectedInterceptors)
        defaultInterceptors.add(
            MonitoringServerInterceptor.create(
                Configuration.allMetrics().withCollectorRegistry(collectorRegistry)
            )
        )
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

    override fun start() {
        healthService.isServing = true
    }

    override fun stop(): CompletableFuture<*> {
        healthService.isServing = false
        return CompletableFuture.completedFuture(true)
    }
}