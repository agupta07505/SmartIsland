/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/** A player-defined custom transport action, reduced to what like detection needs. */
data class MediaCustomAction(val id: String, val title: String)

/**
 * Finds a player's "like / favorite" toggle among its custom actions and reads the state its label
 * reports ("Add to favorites" = not liked, "Remove from favorites" = liked).
 *
 * A player toggles a favorite with exactly one mechanism. Sending both a rating and the custom action
 * flips it twice and leaves it unchanged, so callers must pick one: the custom action when the
 * player offers it, a heart rating otherwise.
 */
object MediaLike {
    private val toggleWords = listOf("favorite", "favourite", "like", "heart", "love", "thumb")
    private val activeWords = listOf(
        "remove", "unlike", "un-like", "unfavorite", "unfavourite", "unheart", "liked", "favorited", "saved"
    )

    private fun text(action: MediaCustomAction): String = "${action.id} ${action.title}".lowercase()

    fun findAction(actions: List<MediaCustomAction>): MediaCustomAction? =
        actions.firstOrNull { action ->
            val t = text(action).replace("dislike", "")
            toggleWords.any { it in t }
        }

    /** True when the action's own label says the track is currently liked. */
    fun isActive(action: MediaCustomAction): Boolean {
        val t = text(action).replace("dislike", "")
        return activeWords.any { it in t }
    }
}
