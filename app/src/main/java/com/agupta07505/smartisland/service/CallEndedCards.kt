/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.model.CallEnds
import com.agupta07505.smartisland.model.IslandTimings
import com.agupta07505.smartisland.util.CallHangUps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class CallEndedCards(
    private val scope: CoroutineScope,
    private val repository: () -> INotificationRepository
) {
    private val notificationRepository: INotificationRepository get() = repository()
    private var lingerJob: Job? = null

    /**
     * The card [key] is gone from the island. An answered call leaves a call-end card in its place for a short
     * linger, every other card just goes. [endedAtMs] is when the call's notification went, not when this runs
     */
    fun remove(key: String, endedAtMs: Long = System.currentTimeMillis()) {
        val card = notificationRepository.notifications.value.firstOrNull { it.key == key }
        notificationRepository.removeNotification(key)
        if (card == null || !CallEnds.canEnd(card)) return
        val reason = CallEnds.reasonOf(CallHangUps.consume(key), endedAtMs)
        notificationRepository.postNotification(CallEnds.cardOf(card, reason, endedAtMs))
        lingerJob?.cancel()
        lingerJob = scope.launch {
            delay(IslandTimings.CALL_ENDED_CARD_MS)
            notificationRepository.removeNotification(CallEnds.CARD_KEY)
        }
    }
}
