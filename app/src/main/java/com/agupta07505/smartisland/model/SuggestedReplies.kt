/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

object SuggestedReplies {
    /** Android shows at most 3, more would crowd the card. */
    const val MAX = 3

    /** Longer ones would not fit a chip and are not quick replies. */
    const val MAX_LENGTH = 40

    class ActionChoices(
        val hasRemoteInput: Boolean,
        val sendable: Boolean,
        val choices: List<CharSequence?>
    )

    /**
     * The preset choices of the one action a tapped chip is sent through (the first sendable one that takes text),
     * never another action's, so a chip always goes where its text was offered
     */
    fun presetChoices(actions: List<ActionChoices>): List<CharSequence?> =
        actions.firstOrNull { it.hasRemoteInput && it.sendable }?.choices.orEmpty()

    fun merge(assistant: List<CharSequence?>, app: List<CharSequence?>): List<String> =
        (assistant + app)
            .mapNotNull { it?.toString()?.trim() }
            .filter { it.isNotEmpty() && it.length <= MAX_LENGTH }
            .distinctBy { it.lowercase() }
            .take(MAX)
}
