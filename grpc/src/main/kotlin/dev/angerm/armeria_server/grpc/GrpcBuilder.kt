package dev.angerm.armeria_server.grpc

import com.google.inject.Inject
import com.linecorp.armeria.server.ServerBuilder
import com.linecorp.armeria.server.grpc.GrpcService
import dev.angerm.armeria_server.ArmeriaAddon
import io.grpc.ServerServiceDefinition

class GrpcBuilder: ArmeriaAddon {
    private val builder = GrpcService.builder()
    @Inject(optional = true) private val bindableServices: List<ServerServiceDefinition> = listOf()

    private fun build(): GrpcService {
        bindableServices.forEach {
            builder.addService(it)
        }
        return builder.build()
    }

    override fun apply(builder: ServerBuilder) {
        builder.service(build())
    }
}