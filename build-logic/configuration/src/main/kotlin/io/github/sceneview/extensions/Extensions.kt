package io.github.sceneview.extensions

import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.plugin.use.PluginDependency
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

internal fun Project.getVersionCatalog(): VersionCatalog {
    return extensions.getByType<VersionCatalogsExtension>().named("libs")
}

internal fun VersionCatalog.getVersion(alias: String): String {
    return findVersion(alias).get().requiredVersion
}

internal fun VersionCatalog.getPlugin(alias: String): PluginDependency {
    return findPlugin(alias).get().get()
}

internal fun VersionCatalog.getLibrary(alias: String): MinimalExternalModuleDependency {
    return findLibrary(alias).get().get()
}

internal fun VersionCatalog.getBundle(alias: String): ExternalModuleDependencyBundle {
    return findBundle(alias).get().get()
}
