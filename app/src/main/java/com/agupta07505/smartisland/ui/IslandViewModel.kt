/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import com.agupta07505.smartisland.alert.AlertClassifier
import com.agupta07505.smartisland.alert.AlertEngine
import com.agupta07505.smartisland.alert.AlertTier
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class IslandViewModel(
    private val settingsRepo: SmartIslandSettingsRepository,
    val notificationRepo: INotificationRepository
) : ViewModel() {

    val settings = settingsRepo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SmartIslandSettings.Default
    )

    val notifications = notificationRepo.notifications
    val foregroundPackage = MutableStateFlow<String?>(null)

    // The rules engine decides what the island shows: it drops passive alerts, orders by priority,
    // folds messages from one app into a counted entry, and times messages out after seven seconds.
    private val alertEngine = AlertEngine()
    private val alertTick = MutableStateFlow(0)
    private var alertExpiryJob: Job? = null
    private var alertHoldJob: Job? = null

    val visibleNotifications: StateFlow<List<IslandNotification>> = combine(
        notifications,
        foregroundPackage,
        settings,
        alertTick
    ) { list, fgPkg, s, _ ->
        val allowed = list.filterNot { notif ->
            (!s.enableBatteryMode && notif.mode == IslandMode.Battery) ||
            (!fgPkg.isNullOrEmpty() && notif.mode == IslandMode.Music && notif.packageName == fgPkg)
        }
        val now = System.currentTimeMillis()
        val evaluation = alertEngine.evaluate(allowed, now)
        scheduleAlertExpiry(evaluation.nextExpiryAtMs, now)
        evaluation.visible
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val expanded = MutableStateFlow(false)
    val selectedIndex = MutableStateFlow(0)
    val isLocked = MutableStateFlow(false)
    val isInputActive = MutableStateFlow(false)

    val mode: StateFlow<IslandMode> = combine(visibleNotifications, selectedIndex) { list, idx ->
        list.getOrNull(idx)?.mode ?: IslandMode.Empty
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = IslandMode.Empty
    )

    private var autoCollapseJob: Job? = null

    init {
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Notifications collector failed") {
                var lastPrimaryKey: String? = null
                visibleNotifications.collect { list ->
                    // A new main pill (a higher tier arrived, or the old one ended) takes the island back from any
                    // swap the user made; otherwise their choice stands.
                    val primaryKey = list.firstOrNull()?.key
                    val primaryChanged = primaryKey != lastPrimaryKey
                    lastPrimaryKey = primaryKey
                    selectedIndex.update { currentSelected ->
                        if (primaryChanged) 0 else currentSelected.coerceIn(0, (list.size - 1).coerceAtLeast(0))
                    }
                }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Auto-expand collector failed") {
                notificationRepo.autoExpandEvent.collect { key ->
                    val notif = notifications.value.firstOrNull { it.key == key } ?: return@collect
                    val isFgMusic = notif.mode == IslandMode.Music &&
                        !foregroundPackage.value.isNullOrEmpty() &&
                        notif.packageName == foregroundPackage.value
                    if (isFgMusic) return@collect

                    // Collision: a message that arrives during a call, an alarm or another live task never
                    // takes the primary island; it waits behind it as a secondary alert.
                    if (AlertClassifier.classify(notif) == AlertTier.Passive) return@collect
                    // The engine's list can lag the raw one by a beat: wait for this alert to show up in it.
                    val visible = withTimeoutOrNull(ALERT_VISIBLE_WAIT_MS) {
                        visibleNotifications.first { list -> list.any { it.key == key || key in it.memberKeys } }
                    } ?: visibleNotifications.value
                    val primary = visible.firstOrNull()
                    if (primary != null &&
                        AlertClassifier.classify(notif) == AlertTier.Ephemeral &&
                        AlertClassifier.classify(primary).let { it == AlertTier.Critical || it == AlertTier.Persistent }
                    ) return@collect

                    // Indexes the paged island list, where a folded entry stands for several keys.
                    val index = visible.indexOfFirst { it.key == key || key in it.memberKeys }
                    if (index >= 0) {
                        selectedIndex.value = index
                        val isDemo = key.startsWith("demo_")
                        if (settings.value.autoExpandOnNotification || isDemo) {
                            expand()
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Timer-reset collector failed") {
                notificationRepo.resetTimerEvent.collect {
                    resetAutoCollapseTimer()
                    onUserInteraction()
                }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Expanded-state collector failed") {
                expanded.collect { isExpanded ->
                    // While the card is open the seven-second clock is held; it restarts when it closes.
                    alertHoldJob?.cancel()
                    if (isExpanded) {
                        alertHoldJob = viewModelScope.launch {
                            while (true) {
                                alertEngine.touch(System.currentTimeMillis())
                                delay(ALERT_HOLD_INTERVAL_MS)
                            }
                        }
                    } else {
                        onUserInteraction()
                    }
                    if (isExpanded) {
                        if (!isInputActive.value) {
                            startAutoCollapseTimer()
                        }
                    } else {
                        stopAutoCollapseTimer()
                        isInputActive.value = false
                    }
                }
            }
        }
        viewModelScope.launch {
            runSuspendCatchingLogged(TAG, "Input-active collector failed") {
                isInputActive.collect { active ->
                    if (active) {
                        stopAutoCollapseTimer()
                    } else if (expanded.value) {
                        startAutoCollapseTimer()
                    }
                }
            }
        }
    }

    fun expand() {
        expanded.value = true
    }

    fun collapse() {
        isInputActive.value = false
        expanded.value = false
    }

    private val immersiveState = MutableStateFlow(false)

    /** A full-screen app has hidden the status bar (Rule_Book 10.1). */
    val isImmersive: StateFlow<Boolean> = immersiveState

    /** SUPPRESSED: immersive with nothing allowed through, so the island fades out and lets touches pass. */
    val isSuppressed: StateFlow<Boolean> = combine(immersiveState, visibleNotifications) { immersive, list ->
        immersive && list.isEmpty()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Fed by the overlay service's system-bar probe; only alarms and incoming calls show while true. */
    fun setImmersive(immersive: Boolean) {
        if (alertEngine.immersive == immersive) return
        alertEngine.immersive = immersive
        immersiveState.value = immersive
        alertTick.update { it + 1 }
    }

    /** The lock screen's face-unlock window (Rule_Book 8.2): the island holds IDLE while true. */
    fun setBiometricClearance(active: Boolean) {
        if (alertEngine.biometricHold == active) return
        alertEngine.biometricHold = active
        if (active) collapse()
        alertTick.update { it + 1 }
    }

    /** The user touched the island: pending messages get their full seven seconds again. */
    fun onUserInteraction() {
        alertEngine.touch(System.currentTimeMillis())
        alertTick.update { it + 1 }
    }

    private fun scheduleAlertExpiry(expiresAtMs: Long?, nowMs: Long) {
        alertExpiryJob?.cancel()
        alertExpiryJob = null
        if (expiresAtMs == null) return
        alertExpiryJob = viewModelScope.launch {
            delay((expiresAtMs - nowMs).coerceAtLeast(0L) + ALERT_EXPIRY_SLACK_MS)
            alertTick.update { it + 1 }
        }
    }

    fun setInputActive(active: Boolean) {
        if (isInputActive.value != active) {
            isInputActive.value = active
        }
    }

    private var lastToggleTimeMs = 0L

    fun toggleExpanded() {
        val now = System.currentTimeMillis()
        if (now - lastToggleTimeMs < 350L) return
        lastToggleTimeMs = now
        expanded.update { !it }
    }

    private fun startAutoCollapseTimer() {
        autoCollapseJob?.cancel()
        if (isInputActive.value) return
        val delayMs = if (mode.value == IslandMode.Music) MUSIC_COLLAPSE_DELAY_MS else AUTO_COLLAPSE_DELAY_MS
        autoCollapseJob = viewModelScope.launch {
            delay(delayMs)
            collapse()
        }
    }

    private fun stopAutoCollapseTimer() {
        autoCollapseJob?.cancel()
        autoCollapseJob = null
    }

    fun resetAutoCollapseTimer() {
        if (expanded.value) {
            startAutoCollapseTimer()
        }
    }

    fun setSelectedNotificationIndex(index: Int) {
        val list = visibleNotifications.value
        if (index in list.indices) {
            selectedIndex.value = index
            resetAutoCollapseTimer()
        }
    }

    fun dismissCurrentNotification() {
        val list = visibleNotifications.value
        val index = selectedIndex.value
        if (index in list.indices) {
            val notification = list[index]
            // A folded entry dismisses every message it stands for.
            val keys = notification.memberKeys.ifEmpty { listOf(notification.key) }
            keys.forEach { key ->
                notificationRepo.removeNotification(key)
                notificationRepo.sendCommand(SmartIslandCommand.CancelNotification(key))
            }
        }
        collapse()
    }

    fun dismissAllNotifications() {
        val list = visibleNotifications.value
        for (notification in list) {
            notificationRepo.sendCommand(SmartIslandCommand.CancelNotification(notification.key))
        }
        notificationRepo.removeAllNotifications()
        collapse()
    }

    companion object {
        private const val TAG = "IslandViewModel"
        private const val AUTO_COLLAPSE_DELAY_MS = 5000L
        private const val MUSIC_COLLAPSE_DELAY_MS = 10_000L
        private const val ALERT_HOLD_INTERVAL_MS = 1_000L
        private const val ALERT_EXPIRY_SLACK_MS = 20L
        private const val ALERT_VISIBLE_WAIT_MS = 500L

        fun provideFactory(
            settingsRepo: SmartIslandSettingsRepository,
            notificationRepo: INotificationRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return IslandViewModel(settingsRepo, notificationRepo) as T
            }
        }
    }
}
