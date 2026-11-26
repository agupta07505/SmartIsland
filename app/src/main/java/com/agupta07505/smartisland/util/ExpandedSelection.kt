/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification

/**
 * What an open island does when its card disappears by itself. Only a new notification may open the island on its own,
 * so an old card from the stack never shows up expanded in its place: the island closes instead
 */
object ExpandedSelection {

    sealed interface Outcome {
        data object Keep : Outcome

        data object Collapse : Outcome

        data class Select(val index: Int) : Outcome
    }

    private fun isReplacedUnderNewKey(card: IslandNotification): Boolean =
        card.mode == IslandMode.IncomingCall && (card.isCallRinging || card.isCallConnecting)

    /** [previous] and [current] are the lists the island shows, [selectedIndex] is the card shown in [previous]. */
    fun afterSelectedGone(
        previous: List<IslandNotification>,
        selectedIndex: Int,
        current: List<IslandNotification>
    ): Outcome {
        val gone = previous.getOrNull(selectedIndex) ?: return Outcome.Keep
        if (current.any { it.key == gone.key }) return Outcome.Keep
        if (isReplacedUnderNewKey(gone)) return Outcome.Keep
        // Only an ordinary notification may be replaced by the same app's new card. A live event that ended closes the
        // island, whatever gets posted next
        if (gone.mode != IslandMode.Notification) return Outcome.Collapse
        val previousKeys = previous.mapTo(HashSet()) { it.key }
        val replacement = current.indexOfFirst { it.key !in previousKeys && it.packageName == gone.packageName }
        return if (replacement >= 0) Outcome.Select(replacement) else Outcome.Collapse
    }

    /**
     * An open island showing the screen recorder closes when the recording ends. Nothing waiting in the list (old,
     * already auto-expanded or switched off) may take its place
     */
    fun recorderEnded(previous: List<IslandNotification>, selectedIndex: Int,
            current: List<IslandNotification>): Boolean {
        val shown = previous.getOrNull(selectedIndex) ?: return false
        if (shown.mode != IslandMode.ScreenRecording) return false
        if (current.any { it.key == shown.key && it.mode == IslandMode.ScreenRecording }) return false
        return current.none { it.mode == IslandMode.IncomingCall && (it.isCallRinging || it.isCallConnecting) }
    }
}

/** What the selection toolbar's Share and Search send to the system, built here so the island never owns a screen. */
object SelectionActions {
    /** Longest text handed to another app, a whole long notification would overflow the intent. */
    const val MAX_CHARS = 20_000

    fun clean(text: String): String = text.trim().take(MAX_CHARS)

    fun shareIntent(text: String): Intent {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, clean(text))
        return Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun searchIntent(text: String): Intent =
        Intent(Intent.ACTION_WEB_SEARCH)
            .putExtra(SearchManager.QUERY, clean(text).take(MAX_QUERY_CHARS))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** No search app answers ACTION_WEB_SEARCH: the same query in a browser. */
    fun browserSearchIntent(text: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" +
            Uri.encode(clean(text).take(MAX_QUERY_CHARS))))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private const val MAX_QUERY_CHARS = 500

    /** True when something took the text */
    fun share(context: Context, text: String): Boolean = start(context, shareIntent(text))

    fun search(context: Context, text: String): Boolean =
        start(context, searchIntent(text)) || start(context, browserSearchIntent(text))

    internal fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
