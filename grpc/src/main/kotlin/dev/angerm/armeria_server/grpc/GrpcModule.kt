package dev.angerm.armeria_server.grpc

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Provides

class GrpcModule: AbstractModule() {
    @Provides
    @Inject
    fun getBuilder(
        impl: GrpcBuilder
    ) = impl
}