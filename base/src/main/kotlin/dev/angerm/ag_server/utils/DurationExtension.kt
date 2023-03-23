package dev.angerm.ag_server.utils

import java.time.Duration

@Suppress("MagicNumber")
fun Duration.toSecondsAsDouble(): Double {
   return this.toMillis() / 1000.0
}