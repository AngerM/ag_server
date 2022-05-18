package dev.angerm.ag_server.grpc

import com.google.inject.Inject
import com.google.inject.name.Named
import com.linecorp.armeria.server.ServerBuilder
import com.linecorp.armeria.server.grpc.GrpcService
import com.linecorp.armeria.server.grpc.GrpcServiceBuilder
import com.uchuhimo.konf.Config
import dev.angerm.ag_server.ArmeriaAddon
import dev.angerm.ag_server.Metrics
import dev.angerm.ag_server.grpc.services.HealthService
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.ServerServiceDefinition
import io.grpc.protobuf.services.ProtoReflectionService
import io.prometheus.client.CollectorRegistry
import me.dinowernli.grpc.prometheus.Configuration
import me.dinowernli.grpc.prometheus.MonitoringServerInterceptor
import java.util.concurrent.CompletableFuture

class GrpcBuilder @Inject constructor(
    private val healthService: HealthService,
    private val config: Config,
    collectorRegistry: CollectorRegistry,
) : ArmeriaAddon {
    interface Modifier {
        fun modify(builder: GrpcServiceBuilder, config: Config) {}
    }
    companion object {
        const val GLOBAL_INTERCEPTORS = "Global"
    }
    private val builder = GrpcService.builder()
    @Inject(optional = true) private val bindableServices: Set<ServerServiceDefinition> = setOf()
    @Inject(optional = true) @Named(GLOBAL_INTERCEPTORS) private val injectedInterceptors: Set<ServerInterceptor> = setOf()
    @Inject(optional = true) @Named(GLOBAL_INTERCEPTORS) private val injectedOrderedInterceptors: Set<List<ServerInterceptor>> = setOf()
    @Inject(optional = true) private val modifyGrpcBuilder: Modifier? = null
    private val defaultInterceptors: MutableList<ServerInterceptor> = mutableListOf()

    init {
        builder.addService(ProtoReflectionService.newInstance())
        builder.addService(healthService.bindService())
        builder.maxRequestMessageLength(config[GrpcSpec.maxInboundMessageSizeBytes])
        builder.maxResponseMessageLength(config[GrpcSpec.maxOutboundMessageSizeBytes])
        builder.enableHttpJsonTranscoding(config[GrpcSpec.enableHttpJsonEncoding])
        defaultInterceptors.addAll(injectedInterceptors)
        injectedOrderedInterceptors.forEach { orderedList ->
            // reverse the list to make it 'easier' for our users since the last interceptor is called first
            defaultInterceptors.addAll(orderedList.reversed())
        }
        // the last interceptor so it is called first
        defaultInterceptors.add(
            MonitoringServerInterceptor.create(
                Configuration
                    .allMetrics()
                    .withCollectorRegistry(collectorRegistry)
                    .withLabelHeaders(config[GrpcSpec.headersToLog])
                    .withLatencyBuckets(Metrics.buckets)
            )
        )
    }

    private fun build(): GrpcService {
        bindableServices.forEach {
            builder.addService(
                ServerInterceptors.intercept(it, defaultInterceptors.toList())
            )
        }
        modifyGrpcBuilder?.modify(builder, config)
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