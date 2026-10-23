/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandNotification

/** The island's callbacks and a few values, always as fresh as the latest composition. */
internal class IslandCallbacks(
    private val onToggle: State<() -> Unit>,
    private val hasIdleStoredCards: State<Boolean>,
    private val onExpandedSurfaceBounds: State<(android.graphics.Rect?) -> Unit>,
    private val onRevealIdleCards: State<() -> Unit>,
    private val onIdleDotTap: State<() -> Unit>,
    private val onOpenAppsLauncher: State<() -> Unit>,
    private val onTogglePillHidden: State<() -> Unit>,
    private val onDismiss: State<() -> Unit>,
    private val onDismissAll: State<() -> Unit>,
    private val onOpenFloatingWindow: State<() -> Unit>,
    private val onOpenNotificationShade: State<() -> Unit>,
    private val onOpenNotification: State<(IslandNotification) -> Unit>,
    private val onPageSelected: State<(Int) -> Unit>,
    private val onSwitchEvent: State<(Boolean) -> Unit>,
    private val onSwitchNotification: State<((Boolean) -> Boolean)?>,
    private val onSwitchSibling: State<(Boolean) -> Boolean>,
    private val onOpenSiblingPicker: State<() -> Boolean>,
    private val onSiblingPickerClose: State<() -> Unit>,
    private val siblingPickerOpen: State<Boolean>,
    private val mediaExpansion: State<com.agupta07505.smartisland.ui.expanded.MediaExpansion>,
    private val readingExpandedText: State<Boolean>,
    private val cardDepth: State<com.agupta07505.smartisland.model.CardDepth>,
    private val onCardDeeper: State<() -> Boolean>,
    private val onCardBack: State<() -> Boolean>,
    private val expanded: State<Boolean>,
    private val settings: State<SmartIslandSettings>,
    private val notifications: State<List<IslandNotification>>,
    private val selectedIndex: State<Int>
) {
    val currentOnToggle: () -> Unit get() = onToggle.value
    val currentHasIdleStoredCards: Boolean get() = hasIdleStoredCards.value
    val currentOnExpandedSurfaceBounds: (android.graphics.Rect?) -> Unit get() = onExpandedSurfaceBounds.value
    val currentOnRevealIdleCards: () -> Unit get() = onRevealIdleCards.value
    val currentOnIdleDotTap: () -> Unit get() = onIdleDotTap.value
    val currentOnOpenAppsLauncher: () -> Unit get() = onOpenAppsLauncher.value
    val currentOnTogglePillHidden: () -> Unit get() = onTogglePillHidden.value
    val currentOnDismiss: () -> Unit get() = onDismiss.value
    val currentOnDismissAll: () -> Unit get() = onDismissAll.value
    val currentOnOpenFloatingWindow: () -> Unit get() = onOpenFloatingWindow.value
    val currentOnOpenNotificationShade: () -> Unit get() = onOpenNotificationShade.value
    val currentOnOpenNotification: (IslandNotification) -> Unit get() = onOpenNotification.value
    val currentOnPageSelected: (Int) -> Unit get() = onPageSelected.value
    val currentOnSwitchEvent: (Boolean) -> Unit get() = onSwitchEvent.value
    val currentOnSwitchNotification: ((Boolean) -> Boolean)? get() = onSwitchNotification.value
    val currentOnSwitchSibling: (Boolean) -> Boolean get() = onSwitchSibling.value
    val currentOnOpenSiblingPicker: () -> Boolean get() = onOpenSiblingPicker.value
    val currentOnSiblingPickerClose: () -> Unit get() = onSiblingPickerClose.value
    val currentSiblingPickerOpen: Boolean get() = siblingPickerOpen.value
    val currentMediaExpansion: com.agupta07505.smartisland.ui.expanded.MediaExpansion get() = mediaExpansion.value
    val currentReadingExpandedText: Boolean get() = readingExpandedText.value
    val currentCardDepth: com.agupta07505.smartisland.model.CardDepth get() = cardDepth.value
    val currentOnCardDeeper: () -> Boolean get() = onCardDeeper.value
    val currentOnCardBack: () -> Boolean get() = onCardBack.value
    val currentExpanded: Boolean get() = expanded.value
    val currentSettings: SmartIslandSettings get() = settings.value
    val currentNotifications: List<IslandNotification> get() = notifications.value
    val currentSelectedIndex: Int get() = selectedIndex.value
}

@Composable
internal fun rememberIslandCallbacks(
    onToggleExpanded: () -> Unit,
    hasIdleStoredCards: Boolean,
    onExpandedSurfaceBounds: (android.graphics.Rect?) -> Unit,
    onRevealIdleCards: () -> Unit,
    onIdleDotTap: () -> Unit,
    onOpenAppsLauncher: () -> Unit,
    onTogglePillHidden: () -> Unit,
    onDismissNotification: () -> Unit,
    onDismissAllNotifications: () -> Unit,
    onOpenFloatingWindow: () -> Unit,
    onOpenNotificationShade: () -> Unit,
    onOpenNotification: (IslandNotification) -> Unit,
    onPageSelected: (Int) -> Unit,
    onSwitchEvent: (Boolean) -> Unit,
    onSwitchNotification: ((Boolean) -> Boolean)?,
    onSwitchSibling: (Boolean) -> Boolean,
    onOpenSiblingPicker: () -> Boolean,
    onSiblingPickerClose: () -> Unit,
    siblingPickerOpen: Boolean,
    mediaExpansion: com.agupta07505.smartisland.ui.expanded.MediaExpansion,
    readingExpandedText: Boolean,
    cardDepth: com.agupta07505.smartisland.model.CardDepth,
    onCardDeeper: () -> Boolean,
    onCardBack: () -> Boolean,
    expanded: Boolean,
    settings: SmartIslandSettings,
    notifications: List<IslandNotification>,
    safeIndex: Int
): IslandCallbacks {
    val onToggleState = rememberUpdatedState(onToggleExpanded)
    val hasIdleStoredCardsState = rememberUpdatedState(hasIdleStoredCards)
    val onExpandedSurfaceBoundsState = rememberUpdatedState(onExpandedSurfaceBounds)
    val onRevealIdleCardsState = rememberUpdatedState(onRevealIdleCards)
    val onIdleDotTapState = rememberUpdatedState(onIdleDotTap)
    val onOpenAppsLauncherState = rememberUpdatedState(onOpenAppsLauncher)
    val onTogglePillHiddenState = rememberUpdatedState(onTogglePillHidden)
    val onDismissState = rememberUpdatedState(onDismissNotification)
    val onDismissAllState = rememberUpdatedState(onDismissAllNotifications)
    val onOpenFloatingWindowState = rememberUpdatedState(onOpenFloatingWindow)
    val onOpenNotificationShadeState = rememberUpdatedState(onOpenNotificationShade)
    val onOpenNotificationState = rememberUpdatedState(onOpenNotification)
    val onPageSelectedState = rememberUpdatedState(onPageSelected)
    val onSwitchEventState = rememberUpdatedState(onSwitchEvent)
    val onSwitchNotificationState = rememberUpdatedState(onSwitchNotification)
    val onSwitchSiblingState = rememberUpdatedState(onSwitchSibling)
    val onOpenSiblingPickerState = rememberUpdatedState(onOpenSiblingPicker)
    val onSiblingPickerCloseState = rememberUpdatedState(onSiblingPickerClose)
    val siblingPickerOpenState = rememberUpdatedState(siblingPickerOpen)
    val mediaExpansionState = rememberUpdatedState(mediaExpansion)
    val readingExpandedTextState = rememberUpdatedState(readingExpandedText)
    val cardDepthState = rememberUpdatedState(cardDepth)
    val onCardDeeperState = rememberUpdatedState(onCardDeeper)
    val onCardBackState = rememberUpdatedState(onCardBack)
    val expandedState = rememberUpdatedState(expanded)
    val settingsState = rememberUpdatedState(settings)
    val notificationsState = rememberUpdatedState(notifications)
    val selectedIndexState = rememberUpdatedState(safeIndex)
    return remember {
        IslandCallbacks(
            onToggle = onToggleState,
            hasIdleStoredCards = hasIdleStoredCardsState,
            onExpandedSurfaceBounds = onExpandedSurfaceBoundsState,
            onRevealIdleCards = onRevealIdleCardsState,
            onIdleDotTap = onIdleDotTapState,
            onOpenAppsLauncher = onOpenAppsLauncherState,
            onTogglePillHidden = onTogglePillHiddenState,
            onDismiss = onDismissState,
            onDismissAll = onDismissAllState,
            onOpenFloatingWindow = onOpenFloatingWindowState,
            onOpenNotificationShade = onOpenNotificationShadeState,
            onOpenNotification = onOpenNotificationState,
            onPageSelected = onPageSelectedState,
            onSwitchEvent = onSwitchEventState,
            onSwitchNotification = onSwitchNotificationState,
            onSwitchSibling = onSwitchSiblingState,
            onOpenSiblingPicker = onOpenSiblingPickerState,
            onSiblingPickerClose = onSiblingPickerCloseState,
            siblingPickerOpen = siblingPickerOpenState,
            mediaExpansion = mediaExpansionState,
            readingExpandedText = readingExpandedTextState,
            cardDepth = cardDepthState,
            onCardDeeper = onCardDeeperState,
            onCardBack = onCardBackState,
            expanded = expandedState,
            settings = settingsState,
            notifications = notificationsState,
            selectedIndex = selectedIndexState
        )
    }
}
