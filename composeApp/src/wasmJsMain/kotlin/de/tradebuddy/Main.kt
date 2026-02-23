package de.tradebuddy

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import de.tradebuddy.time.ensureTimeZoneDatabaseLoaded
import de.tradebuddy.ui.AppRoot
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLLinkElement

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ensureTimeZoneDatabaseLoaded()
    configureWebTabBranding()
    ComposeViewport(document.body!!) {
        AppRoot()
    }
}

private fun configureWebTabBranding() {
    document.title = "TradeBuddy"
    val head = document.head ?: return
    upsertFavicon(head, id = "tradebuddy-favicon-light", href = "favicon-light.svg", media = "(prefers-color-scheme: light)")
    upsertFavicon(head, id = "tradebuddy-favicon-dark", href = "favicon-dark.svg", media = "(prefers-color-scheme: dark)")
}

private fun upsertFavicon(
    head: HTMLElement,
    id: String,
    href: String,
    media: String
) {
    val link = (document.getElementById(id) as? HTMLLinkElement)
        ?: (document.createElement("link") as HTMLLinkElement).also {
            it.id = id
            head.appendChild(it)
        }
    link.rel = "icon"
    link.type = "image/svg+xml"
    link.href = href
    link.media = media
}
