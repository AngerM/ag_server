package dev.angerm.armeria_server

import com.uchuhimo.konf.ConfigSpec

object BaseSpec : ConfigSpec() {
    val port by optional(8080)
}
