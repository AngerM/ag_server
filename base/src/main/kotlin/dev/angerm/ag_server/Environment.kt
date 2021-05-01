package dev.angerm.ag_server

enum class Environment(val env: String) {
    Prod("prod"),
    Staging("staging"),
    Local("local"),
    Test("test")
}
object EnvironmentUtil {
    private val nameToEnv: Map<String, Environment> = Environment.values().map {
        it.env to it
    }.toMap()

    fun getEnvironment(env: String?): Environment = nameToEnv[env] ?: Environment.Local
}