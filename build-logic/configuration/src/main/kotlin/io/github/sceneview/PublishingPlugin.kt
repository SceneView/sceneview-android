//
// Copyright © 2024 Geomagical Labs, Inc. All rights reserved.
//

package io.github.sceneview

import io.github.sceneview.extensions.getPlugin
import io.github.sceneview.extensions.getVersionCatalog
import com.android.build.gradle.LibraryExtension
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.api.publish.PublishingExtension
import org.gradle.authentication.http.HttpHeaderAuthentication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.credentials

class PublishingPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {

        val libs = getVersionCatalog()
        with(pluginManager) {
            apply(libs.getPlugin("publish").pluginId)
        }

        extensions.configure<LibraryExtension> {
            publishing {
                configure<MavenPublishBaseExtension> {
                    configure(
                        AndroidSingleVariantLibrary(
                            sourcesJar = true,
                            publishJavadocJar = false,
                        ),
                    )
//                    coordinates(artifactId = getArtifactId())
                }
                configure<PublishingExtension> {
                    repositories {
                        mavenCentral()
                    }
                }
            }
        }
    }
}
