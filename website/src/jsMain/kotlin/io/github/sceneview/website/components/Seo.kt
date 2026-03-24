package io.github.sceneview.website.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.browser.document
import org.w3c.dom.HTMLMetaElement
import org.w3c.dom.HTMLLinkElement
import org.w3c.dom.asList

private const val SITE_URL = "https://sceneview.github.io"
private const val OG_IMAGE = "${SITE_URL}/og-image.png"

/**
 * Inject SEO-related <head> elements for the current page.
 * Call once at the top of each @Page composable.
 */
@Composable
fun PageHead(
    title: String,
    description: String,
    path: String = "/",
    keywords: String = "SceneView, 3D, AR, augmented reality, Android, iOS, Jetpack Compose, SwiftUI, Filament, RealityKit, SDK"
) {
    val fullTitle = if (path == "/") "SceneView — 3D & AR SDK for Android and iOS"
    else "$title — SceneView"
    val canonicalUrl = "$SITE_URL${path.trimEnd('/')}"

    LaunchedEffect(path) {
        document.title = fullTitle

        // Helper to set or create a meta tag
        fun setMeta(attr: String, attrValue: String, content: String) {
            val existing = document.head?.querySelectorAll("meta[$attr=\"$attrValue\"]")
                ?.asList()?.firstOrNull() as? HTMLMetaElement
            if (existing != null) {
                existing.content = content
            } else {
                val meta = document.createElement("meta") as HTMLMetaElement
                meta.setAttribute(attr, attrValue)
                meta.content = content
                document.head?.appendChild(meta)
            }
        }

        // Standard meta
        setMeta("name", "description", description)
        setMeta("name", "keywords", keywords)

        // Open Graph
        setMeta("property", "og:title", fullTitle)
        setMeta("property", "og:description", description)
        setMeta("property", "og:type", "website")
        setMeta("property", "og:url", canonicalUrl)
        setMeta("property", "og:image", OG_IMAGE)
        setMeta("property", "og:site_name", "SceneView")

        // Twitter Card
        setMeta("name", "twitter:card", "summary_large_image")
        setMeta("name", "twitter:title", fullTitle)
        setMeta("name", "twitter:description", description)
        setMeta("name", "twitter:image", OG_IMAGE)

        // Canonical URL
        val existingCanonical = document.head?.querySelector("link[rel=\"canonical\"]") as? HTMLLinkElement
        if (existingCanonical != null) {
            existingCanonical.href = canonicalUrl
        } else {
            val link = document.createElement("link") as HTMLLinkElement
            link.rel = "canonical"
            link.href = canonicalUrl
            document.head?.appendChild(link)
        }
    }
}
