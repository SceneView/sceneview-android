import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kobweb.application)
}

group = "io.github.sceneview.website"
version = "1.0.0"

kobweb {
    app {
        index {
            description.set("SceneView — 3D & AR SDK for Android, iOS, macOS, visionOS")
        }
    }
}

// Post-process exported HTML to inject responsive CSS into <head>
tasks.register("injectResponsiveCss") {
    dependsOn("kobwebExport")
    doLast {
        val css = """<style>.nav-hamburger{display:none!important}@media(max-width:768px){.nav-links{display:none!important}.nav-hamburger{display:block!important}}@media(min-width:769px){.nav-mobile-menu{display:none!important}}.nav-icon-btn:hover{background-color:rgba(0,0,0,.06)}.nav-link-pill:hover{background-color:rgba(0,0,0,.04)}@media(prefers-color-scheme:dark){.nav-icon-btn:hover{background-color:rgba(255,255,255,.08)}.nav-link-pill:hover{background-color:rgba(255,255,255,.06)}}@media(max-width:860px){.hero-text{text-align:center}.hero-text>div{justify-content:center}.hero-viewer{max-width:100%!important}}.m3-sample-card:hover{transform:translateY(-2px) scale(1.01);box-shadow:0 8px 24px rgba(0,0,0,.15)}</style>"""
        val pagesDir = file(".kobweb/site/pages")
        if (pagesDir.exists()) {
            pagesDir.listFiles()?.filter { it.extension == "html" }?.forEach { htmlFile ->
                val content = htmlFile.readText()
                if (!content.contains("nav-hamburger{display:none")) {
                    htmlFile.writeText(content.replace("</head>", "$css\n</head>"))
                    println("Injected responsive CSS into ${htmlFile.name}")
                }
            }
        }
    }
}

kotlin {
    configAsKobwebApplication("sceneview-website")

    sourceSets {
        jsMain.dependencies {
            implementation(libs.kobweb.core)
            implementation(libs.kobweb.silk)
            implementation(libs.silk.icons.fa)
            implementation(compose.html.core)
            implementation(compose.runtime)
        }
    }
}
