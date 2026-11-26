/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.OtpCodes
import com.agupta07505.smartisland.model.RepliedCards
import com.agupta07505.smartisland.model.SuggestedReplies
import com.agupta07505.smartisland.util.runCatchingLogged

internal class SuggestedReplyReader(
    private val settings: () -> SmartIslandSettings,
    private val repliedCards: RepliedCards,
    private val repository: () -> INotificationRepository,
    private val readActive: () -> Array<StatusBarNotification>?,
    private val currentRanking: () -> RankingMap
) {
    /**
     * The assistant (Enhanced notifications) usually adds its suggested replies a moment after the message was
     * posted, so the cards on the island are updated when the ranking changes
     */
    fun refresh(rankingMap: RankingMap) {
        val repository = repository()
        val cards = repository.notifications.value.filter { it.mode == IslandMode.Notification }
        if (cards.isEmpty()) return
        runCatchingLogged(TAG, "Suggested replies could not be refreshed") {
            val active = readActive()?.associateBy { it.key }.orEmpty()
            cards.forEach { card ->
                val posted = active[card.key]?.notification ?: return@forEach
                val replies = repliesFor(posted, card.key, active.getValue(card.key).postTime, rankingMap)
                val copyAction = copyActionFor(card.key, rankingMap)
                if (replies != card.suggestedReplies || copyAction != card.systemCopyAction) {
                    repository.updateNotification(card.key) {
                        it.copy(suggestedReplies = replies, systemCopyAction = copyAction)
                    }
                }
            }
        }
    }

    /** The app's preset choices and the assistant's suggestions for a message, empty when the setting is off. */
    fun repliesFor(
        notification: Notification,
        key: String,
        postTimeMs: Long,
        rankingMap: RankingMap? = null
    ): List<String> {
        if (!settings().showSuggestedReplies) return emptyList()
        // Already answered: the assistant still holds the old suggestions until the app re-posts
        if (repliedCards.hidesSuggestions(key, postTimeMs)) return emptyList()
        // Only the action chips are sent through (the card's reply action, see NotificationCardReader)
        val choices = SuggestedReplies.presetChoices(notification.actions.orEmpty().map { action ->
            val input = action.remoteInputs?.firstOrNull()
            SuggestedReplies.ActionChoices(
                hasRemoteInput = input != null,
                sendable = action.title != null && action.actionIntent != null,
                choices = input?.choices.orEmpty().asList()
            )
        })
        val assistant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatchingLogged(TAG, "Suggested replies could not be read") {
                val map = rankingMap ?: currentRanking()
                val ranking = NotificationListenerService.Ranking()
                if (map.getRanking(key, ranking)) ranking.smartReplies else null
            }.orEmpty()
        } else {
            emptyList()
        }
        return SuggestedReplies.merge(assistant, choices)
    }

    /**
     * The Copy code action Android's assistant (Enhanced notifications) adds to a one-time code message, which is not
     * among the app's own actions. Null when the chip or the suggested replies are off, or the assistant has none
     */
    fun copyActionFor(key: String, rankingMap: RankingMap? = null): IslandNotificationAction? {
        val current = settings()
        if (!current.showCopyCodeChip || !current.showSuggestedReplies) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatchingLogged(TAG, "Android's copy code action could not be read") {
                val map = rankingMap ?: currentRanking()
                val ranking = NotificationListenerService.Ranking()
                if (!map.getRanking(key, ranking)) return@runCatchingLogged null
                ranking.smartActions.asSequence()
                    .mapNotNull { action ->
                        action.actionIntent?.let { IslandNotificationAction(action.title?.toString().orEmpty(), it) }
                    }
                    .firstOrNull(OtpCodes::isCopyAction)
            }
        } else {
            null
        }
    }

    private companion object {
        const val TAG = "SmartIslandNotificationListener"
    }
}
