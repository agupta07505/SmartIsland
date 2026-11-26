/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import com.agupta07505.smartisland.model.CardLinks
import com.agupta07505.smartisland.model.LinkKind

/** The app a link opens in by default. */
class LinkHandler(val packageName: String)

/** Opens a link, address or number from a card in the app Android would open it with, and tells which app that is. */
object LinkOpener {
    private const val SYSTEM_PACKAGE = "android"

    private fun viewIntent(link: String): Intent {
        val uri = Uri.parse(CardLinks.normalize(link))
        val intent = when (CardLinks.kind(link)) {
            LinkKind.EMAIL, LinkKind.SMS -> Intent(Intent.ACTION_SENDTO, uri)
            LinkKind.PHONE -> Intent(Intent.ACTION_DIAL, uri)
            LinkKind.WEB, LinkKind.APP -> Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        }
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** A chooser is what Android answers with when no app is the default for the link. */
    private fun ResolveInfo?.appPackage(): String? =
        this?.activityInfo?.packageName?.takeUnless { it == SYSTEM_PACKAGE }

    /** Only a web link falls back to the browser, an address or number has no browser to open it in. */
    @Suppress("DEPRECATION")
    private fun defaultBrowser(pm: PackageManager, link: String): String? =
        if (CardLinks.kind(link) != LinkKind.WEB) null
        else pm.resolveActivity(viewIntent("https://"), PackageManager.MATCH_DEFAULT_ONLY).appPackage()

    /**
     * The default app for [link] (a verified app link opens its own app, any other web link the default browser, an
     * address or number the mail, phone or messages app, any other scheme the app that registered it), or null when
     * nothing can open it
     */
    @Suppress("DEPRECATION")
    fun handlerFor(context: Context, link: String): LinkHandler? =
        runCatchingLogged("LinkOpener", "Default app for a link could not be read") {
            val pm = context.packageManager
            val intent = viewIntent(link)
            val pkg = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY).appPackage()
                ?: defaultBrowser(pm, link)
                ?: pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    .firstNotNullOfOrNull { it.appPackage() }
            pkg?.let { LinkHandler(it) }
        }

    fun icon(context: Context, handler: LinkHandler): Drawable? =
        runCatchingLogged("LinkOpener", "App icon for a link could not be read") {
            context.packageManager.getApplicationIcon(handler.packageName)
        }

    /** True when an app took the link. With [handler] it goes to that app first, so it opens where its icon showed. */
    fun open(context: Context, link: String, handler: LinkHandler? = null): Boolean {
        val base = viewIntent(link)
        val targeted = handler?.let { Intent(base).setPackage(it.packageName) }
        return listOfNotNull(targeted, base).any { SelectionActions.start(context, it) }
    }
}
