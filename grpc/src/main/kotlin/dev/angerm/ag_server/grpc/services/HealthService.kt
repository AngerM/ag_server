package dev.angerm.ag_server.grpc.services

import io.grpc.health.v1.HealthCheckRequest
import io.grpc.health.v1.HealthCheckResponse
import io.grpc.health.v1.HealthGrpc
import io.grpc.stub.StreamObserver

open class HealthService : HealthGrpc.HealthImplBase() {
    var isServing = false

    private fun respond(responseObserver: StreamObserver<HealthCheckResponse>?) {
        responseObserver?.onNext(
            HealthCheckResponse.newBuilder().apply {
                this.status = if (isServing) {
                    HealthCheckResponse.ServingStatus.SERVING
                } else {
                    HealthCheckResponse.ServingStatus.NOT_SERVING
                }
            }.build()
        )
        responseObserver?.onCompleted()
    }

    override fun check(request: HealthCheckRequest?, responseObserver: StreamObserver<HealthCheckResponse>?) {
        respond(responseObserver)
    }

    override fun watch(request: HealthCheckRequest?, responseObserver: StreamObserver<HealthCheckResponse>?) {
        respond(responseObserver)
    }
}