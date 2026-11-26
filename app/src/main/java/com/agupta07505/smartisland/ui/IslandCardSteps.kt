/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.model.CardDepth
import com.agupta07505.smartisland.model.CardStepState
import com.agupta07505.smartisland.model.CardSteps
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

/**
 * Open step (preview, reader, picture) of the card the island shows, and the picture open at full size. The view model
 * passes in the card list and selection and keeps the public members as one-line delegates
 */
internal class IslandCardSteps(
    private val scope: CoroutineScope,
    private val visibleNotifications: StateFlow<List<IslandNotification>>,
    private val selectedIndex: StateFlow<Int>,
) {
    val expandedMedia = MutableStateFlow<Pair<String, Int>?>(null)

    fun setExpandedMedia(key: String?, index: Int) {
        // a picture opened directly (a thumbnail tap) is at full size.
        clearPictureHalf()
        expandedMedia.value = key?.let { it to index }
    }

    fun closeExpandedMedia() {
        clearPictureHalf()
        expandedMedia.value = null
    }

    private fun clearPictureHalf() {
        val old = cardStepState.value
        if (old.pictureHalf) cardStepState.value = old.copy(pictureHalf = false)
    }

    private fun stepMediaCount(card: IslandNotification): Int =
        if (card.media.any { it.isSticker }) 0 else card.media.size

    /** What each card last said about its text having more to read, the newest few cards. */
    private val reportedExpandable = object : LinkedHashMap<String, Boolean>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean = size > 32
    }

    /** Open step (preview / reader) of the shown card. */
    val cardStepState = MutableStateFlow(CardStepState())

    internal fun shownCard(): IslandNotification? = visibleNotifications.value.getOrNull(selectedIndex.value)

    private fun currentSteps(card: IslandNotification): CardSteps {
        val picture = expandedMedia.value
        return cardStepState.value.steps(card.key, picture?.first, picture?.second ?: 0)
    }

    val cardDepth: StateFlow<CardDepth> = combine(
        cardStepState, expandedMedia, visibleNotifications, selectedIndex
    ) { step, picture, list, index ->
        val card = list.getOrNull(index)
        step.steps(card?.key, picture?.first, picture?.second ?: 0).depth
    }.distinctUntilChanged().stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = CardDepth.First
    )

    internal fun cardDepthNow(): CardDepth {
        val picture = expandedMedia.value
        val card = visibleNotifications.value.getOrNull(selectedIndex.value)
        return cardStepState.value.steps(card?.key, picture?.first, picture?.second ?: 0).depth
    }

    fun setCardSteps(key: String, steps: CardSteps) {
        val old = cardStepState.value
        cardStepState.value = CardStepState(
            key = key,
            preview = steps.preview,
            reader = steps.reader,
            pictureHalf = steps.pictureHalf,
            selector = steps.selector,
            textExpandable = old.takeIf { it.key == key }?.textExpandable ?: false
        )
        expandedMedia.value = steps.pictureIndex?.let { key to it }
    }

    /** Card says whether its text (no pictures) has more to read. */
    fun reportCardTextExpandable(key: String, expandable: Boolean) {
        // kept per card apart from the step state: that state is cleared whenever the shown card changes, and a card
        // that opens by itself reports while that happens. The card reports once, so a lost answer is never asked again
        reportedExpandable[key] = expandable
        if (shownCard()?.key != key) return
        val old = cardStepState.value
        cardStepState.value = if (old.key == key) old.copy(textExpandable = expandable)
        else CardStepState(key = key, textExpandable = expandable)
    }

    fun cardGoDeeper(): Boolean {
        val card = shownCard() ?: return false
        if (card.mode != IslandMode.Notification) return false
        val steps = currentSteps(card)
        val expandable = reportedExpandable[card.key]
            ?: cardStepState.value.takeIf { it.key == card.key }?.textExpandable ?: false
        val next = CardSteps.deeper(steps, stepMediaCount(card), expandable) ?: return false
        setCardSteps(card.key, next)
        return true
    }

    fun cardGoBack(): Boolean {
        val card = shownCard() ?: return false
        val next = CardSteps.back(currentSteps(card), stepMediaCount(card)) ?: return false
        setCardSteps(card.key, next)
        return true
    }
}
