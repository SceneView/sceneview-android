@file:Suppress("UnstableApiUsage")

import java.io.File
import java.io.File.separator

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://jitpack.io") }
        google()
        mavenCentral()
    }
}

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

/**
 * This file is responsible for loading and including all submodules which declare a
 * build.gradle.kts file in their root. No need to use include("path-to-folder") anymore
 */

val ignored = listOf("build", ".gradle", ".idea", "src", "out", ".git", "tools", "build-logic")
val projectNames = mutableListOf<String>()
rootDir.walk().onEnter { !ignored.contains(it.name) }.filter { it.isDirectory }
    .map { it.path.replace(rootDir.path, "") }.forEach { path ->
        val buildGradleFile = File("$rootDir/$path", "build.gradle.kts")
        if (buildGradleFile.exists()) {
            val pathString = path.replace(separator, ":")

            projectNames.add(pathString)
            include(pathString)
        }
    }
