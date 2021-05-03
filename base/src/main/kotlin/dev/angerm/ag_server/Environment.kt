package dev.angerm.ag_server

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.Stage as GStage

class Environment {
    enum class Stage(val envVar: String) {
        Prod("prod"),
        Staging("staging"),
        Local("local"),
        Test("test")
    }
    companion object {
        private val nameToEnv: Map<String, Stage> = Stage.values().associateBy {
            it.envVar
        }

        fun getStage(env: String?): Stage = nameToEnv[env?.lowercase()] ?: Stage.Local
    }

    val stage = getStage(System.getenv("STAGE"))
    val serviceName = System.getenv("SERVICE_NAME") ?: "UNKNOWN"
    fun getGuiceStage(): GStage {
        return when (stage) {
            in setOf(Stage.Prod, Stage.Staging) -> GStage.PRODUCTION
            else -> GStage.DEVELOPMENT
        }
    }

    fun createAgInjector(vararg modules: Module): Injector {
        return Guice.createInjector(
            this.getGuiceStage(),
            AgModule(this),
            *modules,
        )
    }
}