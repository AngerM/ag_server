package dev.angerm.ag_server.grpc

import com.uchuhimo.konf.ConfigSpec

object GrpcSpec : ConfigSpec() {
    val maxInboundMessageSizeBytes by optional(10 * 1024 * 1024)
    val maxOutboundMessageSizeBytes by optional(10 * 1024 * 1024)
}