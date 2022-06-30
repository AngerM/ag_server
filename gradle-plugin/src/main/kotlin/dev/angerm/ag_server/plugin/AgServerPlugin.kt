package dev.angerm.ag_server.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

data class AgInfoExtension(
    val kotlinVersion: String,
)

class AgServerPlugin: Plugin<Project> {
    override fun apply(p: Project) {
        p.pluginManager.apply {
            apply(KotlinBasePlugin::class.java)
            apply(KotlinPlatformJvmPlugin::class.java)
        }
        p.tasks.withType(KotlinCompile::class.java).forEach {
            it.kotlinOptions.jvmTarget = "11"
        }
        val kotlinVersion = p.buildscript.configurations.getByName("classpath")
            .resolvedConfiguration
            .resolvedArtifacts
            .stream()
            .map {
               it.moduleVersion.id
            }
            .filter{
                it.group == "org.jetbrains.kotlin" && it.name == "kotlin-stdlib"
            }.findAny()
            .map {
                it.version
            }.orElseThrow {
                java.lang.IllegalStateException("Unable to get kotlin version")
            }
        p.extensions.add("agInfo", AgInfoExtension(
            kotlinVersion
        ))
    }
}