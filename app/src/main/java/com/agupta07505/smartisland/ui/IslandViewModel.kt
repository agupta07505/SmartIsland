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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    val expanded = MutableStateFlow(false)
    val selectedIndex = MutableStateFlow(0)
    val windowWidthIsMatchParent = MutableStateFlow(false)

    fun setWindowWidthIsMatchParent(value: Boolean) {
        windowWidthIsMatchParent.value = value
    }

    val mode: StateFlow<IslandMode> = combine(notifications, selectedIndex) { list, idx ->
        list.getOrNull(idx)?.mode ?: IslandMode.Empty
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = IslandMode.Empty
    )

    private var autoCollapseJob: Job? = null

    init {
        viewModelScope.launch {
            notifications.collect { list ->
                val currentSelected = selectedIndex.value
                if (currentSelected >= list.size) {
                    selectedIndex.value = (list.size - 1).coerceAtLeast(0)
                }
            }
        }
        viewModelScope.launch {
            notificationRepo.autoExpandEvent.collect { key ->
                val list = notifications.value
                val index = list.indexOfFirst { it.key == key }
                if (index >= 0) {
                    selectedIndex.value = index
                    expand()
                }
            }
        }
        viewModelScope.launch {
            notificationRepo.resetTimerEvent.collect {
                resetAutoCollapseTimer()
            }
        }
        viewModelScope.launch {
            expanded.collect { isExpanded ->
                if (isExpanded) {
                    startAutoCollapseTimer()
                } else {
                    stopAutoCollapseTimer()
                }
            }
        }
    }

    fun expand() {
        expanded.value = true
    }

    fun collapse() {
        expanded.value = false
    }

    private var lastToggleTimeMs = 0L

    fun toggleExpanded() {
        val now = System.currentTimeMillis()
        if (now - lastToggleTimeMs < 350L) return
        lastToggleTimeMs = now
        expanded.value = !expanded.value
    }

    private fun startAutoCollapseTimer() {
        autoCollapseJob?.cancel()
        autoCollapseJob = viewModelScope.launch {
            delay(AUTO_COLLAPSE_DELAY_MS)
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
        val list = notifications.value
        if (index in list.indices) {
            selectedIndex.value = index
            resetAutoCollapseTimer()
        }
    }

    fun dismissCurrentNotification() {
        val list = notifications.value
        val index = selectedIndex.value
        if (index in list.indices) {
            val notification = list[index]
            notificationRepo.removeNotification(notification.key)
            notificationRepo.sendCommand(SmartIslandCommand.CancelNotification(notification.key))
        }
        collapse()
    }

    companion object {
        private const val AUTO_COLLAPSE_DELAY_MS = 5000L

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
