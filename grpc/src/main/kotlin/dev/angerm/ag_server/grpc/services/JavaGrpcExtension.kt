package dev.angerm.ag_server.grpc.services

import com.google.common.util.concurrent.ListenableFuture
import com.spotify.futures.ListenableFuturesExtra
import java.util.concurrent.CompletableFuture

fun <T> ListenableFuture<T>.toCompletableFuture(): CompletableFuture<T> {
    return ListenableFuturesExtra.toCompletableFuture(this)
}