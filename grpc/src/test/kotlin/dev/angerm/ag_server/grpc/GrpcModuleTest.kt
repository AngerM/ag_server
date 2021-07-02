package dev.angerm.ag_server.grpc

import dev.angerm.ag_server.App
import dev.angerm.ag_server.grpc.services.toCompletableFuture
import io.grpc.health.v1.HealthCheckRequest
import io.grpc.health.v1.HealthCheckResponse
import io.grpc.health.v1.HealthGrpc
import kotlinx.coroutines.future.await
import kotlin.test.Test
import kotlin.test.assertEquals

class GrpcModuleTest {
    @Test fun testHealth() = App.testServer(GrpcModule()) { server ->
        val client = server.getGrpcClient(HealthGrpc.HealthBlockingStub::class.java)
        val healthy = client.check(HealthCheckRequest.getDefaultInstance())
        assertEquals(HealthCheckResponse.ServingStatus.SERVING, healthy.status)
    }

    @Test fun testHealthFuture() = App.testServer(GrpcModule()) { server ->
        val client = server.getGrpcClient(HealthGrpc.HealthFutureStub::class.java)
        val healthy = client.check(HealthCheckRequest.getDefaultInstance())
            .toCompletableFuture()
            .await()
        assertEquals(HealthCheckResponse.ServingStatus.SERVING, healthy.status)
    }
}