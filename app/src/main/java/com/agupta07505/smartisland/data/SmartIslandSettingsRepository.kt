/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.agupta07505.smartisland.model.CutoutArea
import com.agupta07505.smartisland.model.CutoutFitPlan
import com.agupta07505.smartisland.model.EdgePillFit
import com.agupta07505.smartisland.model.EdgePillTuning
import com.agupta07505.smartisland.model.EventColors
import com.agupta07505.smartisland.model.FloatingWindowMode
import com.agupta07505.smartisland.model.FoldPosture
import com.agupta07505.smartisland.model.GestureSensitivity
import com.agupta07505.smartisland.model.IdleSchedule
import com.agupta07505.smartisland.model.IdleSlots
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.LandscapeLayoutStyle
import com.agupta07505.smartisland.model.LedTick
import com.agupta07505.smartisland.model.MergeStyle
import com.agupta07505.smartisland.model.NetSpeedRules
import com.agupta07505.smartisland.model.UtilityLayout
import com.agupta07505.smartisland.model.UtilityPlacement
import com.agupta07505.smartisland.model.WeatherConfig
import com.agupta07505.smartisland.model.WeatherPill
import com.agupta07505.smartisland.model.WeatherReadouts
import com.agupta07505.smartisland.model.WeatherRules
import com.agupta07505.smartisland.model.WidgetIslandContent
import com.agupta07505.smartisland.model.WidgetIslandTimer
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.AppAutoExpand
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.BulkAppAction
import com.agupta07505.smartisland.util.CameraCutoutDetector
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.runSuspendCatching
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

private val Context.smartIslandDataStore by preferencesDataStore(
    name = "smart_island_settings",
    corruptionHandler = ReplaceFileCorruptionHandler {
        AppLogRecorder.e("SmartIslandSettings", "settings corrupted, restoring defaults")
        emptyPreferences()
    }
)

/** The read side of the settings: what the card feed needs, so it can run on a fake instead of DataStore. */
interface SettingsSource {
    val settings: Flow<SmartIslandSettings>
    fun forPosture(posture: FoldPosture?): SettingsSource
}

/** App's settings. [profile] is null for the normal ones. */
class SmartIslandSettingsRepository(private val context: Context, profile: FoldPosture? = null) : SettingsSource {
    /** Fold mode this repository is bound to, or null for the normal settings. */
    private val activeProfile: FoldPosture? = profile?.takeIf { it != FoldPosture.Standard }

    private val profileRepos = ConcurrentHashMap<FoldPosture, SmartIslandSettingsRepository>()

    override fun forPosture(posture: FoldPosture?): SmartIslandSettingsRepository {
        val wanted = posture?.takeIf { it != FoldPosture.Standard }
        if (wanted == activeProfile) return this
        return profileRepos.getOrPut(wanted ?: FoldPosture.Standard) { SmartIslandSettingsRepository(context, wanted) }
    }

    /** Runs [migrate] once: the flag is checked before and again inside the edit, and set by the same edit. */
    private suspend fun migrateOnce(done: Preferences.Key<Boolean>, migrate: (MutablePreferences) -> Unit) {
        val prefs = runSuspendCatching { context.smartIslandDataStore.data.first() }.getOrNull() ?: return
        if (prefs[done] == true) return
        editSafely {
            if (it[done] == true) return@editSafely
            migrate(it)
            it[done] = true
        }
    }

    private suspend fun migrateLandscapeShapeOnce() =
        migrateOnce(SettingsKeys.LandscapePillMigrated) {
            it[SettingsKeys.LandscapeShape] = SmartIslandSettings.LANDSCAPE_SHAPE_PILL
        }

    private suspend fun migrateEdgePillSizeOnce() =
        migrateOnce(SettingsKeys.EdgePillPortraitSize) {
            it[SettingsKeys.EdgePill]?.let { saved ->
                it[SettingsKeys.EdgePill] = EdgePillTuning
                    .decode(saved, flatEdgeDefault = teardropPhone).withPortraitSizes().coerced().encode()
            }
        }

    private val teardropPhone: Boolean
        get() = CameraCutoutDetector.isTeardrop(context)

    override val settings: Flow<SmartIslandSettings> = context.smartIslandDataStore.data
        .onStart { if (activeProfile == null) { migrateLandscapeShapeOnce(); migrateEdgePillSizeOnce() } }
        .catch { error ->
            if (error is IOException) {
                AppLogRecorder.e(TAG, "could not read settings, using defaults", error)
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { stored ->
            // a fold mode reads its own values over the normal ones
            val prefs: Preferences = activeProfile?.let { FoldProfiles.view(stored, it) } ?: stored
            readSettings(prefs, teardropPhone)
        }

    suspend fun setDeviceType(value: String) = put(SettingsKeys.DeviceType, value)
    suspend fun setEnabled(value: Boolean) = put(SettingsKeys.Enabled, value)
    suspend fun setEnableShadow(value: Boolean) = put(SettingsKeys.EnableShadow, value)
    suspend fun setExpandedBlurMode(mode: ExpandedBlurMode) = editSafely {
        if (it[SettingsKeys.MediaBlurEnabled] == null) {
            it[SettingsKeys.MediaBlurEnabled] = it[SettingsKeys.ExpandedBlurEnabled]
                ?: SmartIslandSettings.Default.mediaBlurEnabled
        }
        if (it[SettingsKeys.MediaBlurRadius] == null) {
            it[SettingsKeys.MediaBlurRadius] = (it[SettingsKeys.ExpandedBlurRadius]
                ?: SmartIslandSettings.Default.mediaBlurRadius)
                .coerceIn(SmartIslandSettings.MIN_EXPANDED_BLUR, SmartIslandSettings.MAX_EXPANDED_BLUR)
        }
        it[SettingsKeys.ExpandedBlurEnabled] = mode == ExpandedBlurMode.Card
    }
    suspend fun setMediaBlurEnabled(value: Boolean) = put(SettingsKeys.MediaBlurEnabled, value)
    suspend fun setMediaBlurRadius(value: Float) = putClamped(SettingsKeys.MediaBlurRadius, value,
            SmartIslandSettings.MIN_EXPANDED_BLUR, SmartIslandSettings.MAX_EXPANDED_BLUR)
    suspend fun setMediaOpacity(value: Float) = putDimension(SettingsKeys.MediaOpacity, value,
            SmartIslandSettings.Default.mediaOpacity, SmartIslandSettings.MIN_OPACITY, SmartIslandSettings.MAX_OPACITY)
    suspend fun setExpandedBlurRadius(value: Float) = putClamped(SettingsKeys.ExpandedBlurRadius, value,
            SmartIslandSettings.MIN_EXPANDED_BLUR, SmartIslandSettings.MAX_EXPANDED_BLUR)
    suspend fun setExpandedGradientEnabled(value: Boolean) = put(SettingsKeys.ExpandedGradientEnabled, value)
    suspend fun setExpandedGradientColor(value: Long) = putColor(SettingsKeys.ExpandedGradientColor, value,
            SmartIslandSettings.Default.expandedGradientColor)
    suspend fun setExpandedGradientStrength(value: Float) = putClamped(SettingsKeys.ExpandedGradientStrength, value,
            SmartIslandSettings.MIN_GRADIENT_STRENGTH, SmartIslandSettings.MAX_GRADIENT_STRENGTH)
    suspend fun setShadowElevation(value: Float) = putDimension(SettingsKeys.ShadowElevation, value,
            SmartIslandSettings.Default.shadowElevation, SmartIslandSettings.MIN_SHADOW_ELEVATION,
            SmartIslandSettings.MAX_SHADOW_ELEVATION)
    suspend fun setPosition(
        width: Float,
        height: Float,
        xOffset: Float,
        yOffset: Float
    ) = editSafely {
        it[SettingsKeys.Width] = validDimension(width, SmartIslandSettings.Default.width, SmartIslandSettings.MIN_WIDTH,
            SmartIslandSettings.MAX_WIDTH)
        it[SettingsKeys.Height] = validDimension(height, SmartIslandSettings.Default.height,
            SmartIslandSettings.MIN_HEIGHT, SmartIslandSettings.MAX_HEIGHT)
        it[SettingsKeys.XOffset] = validDimension(xOffset, SmartIslandSettings.Default.xOffset,
            SmartIslandSettings.MIN_X_OFFSET, SmartIslandSettings.MAX_X_OFFSET)
        it[SettingsKeys.YOffset] = validDimension(yOffset, SmartIslandSettings.Default.yOffset,
            SmartIslandSettings.MIN_Y_OFFSET, SmartIslandSettings.MAX_Y_OFFSET)
    }
    suspend fun setCornerRadius(value: Float) = putDimension(SettingsKeys.CornerRadius, value,
            SmartIslandSettings.Default.cornerRadius, SmartIslandSettings.MIN_CORNER_RADIUS,
            SmartIslandSettings.MAX_CORNER_RADIUS)
    suspend fun setOpacity(value: Float) = putDimension(SettingsKeys.Opacity, value,
            SmartIslandSettings.Default.opacity, SmartIslandSettings.MIN_OPACITY, SmartIslandSettings.MAX_OPACITY)
    suspend fun setOledBlack(value: Boolean) = put(SettingsKeys.OledBlack, value)

    suspend fun setPillColor(value: Long) = putColor(SettingsKeys.PillColor, value,
            SmartIslandSettings.Default.pillColor)
    suspend fun setBatteryColor(value: Long) = putColor(SettingsKeys.BatteryColor, value,
            SmartIslandSettings.Default.batteryColor)
    suspend fun setNotificationDotColor(value: Long) = putColor(SettingsKeys.NotificationDotColor, value,
            SmartIslandSettings.Default.notificationDotColor)
    suspend fun setMusicVisualizerColor(value: Long) = putColor(SettingsKeys.MusicVisualizerColor, value,
            SmartIslandSettings.Default.musicVisualizerColor)
    suspend fun setHotspotColor(value: Long) = putColor(SettingsKeys.HotspotColor, value,
            SmartIslandSettings.Default.hotspotColor)
    suspend fun setCallColor(value: Long) = putColor(SettingsKeys.CallColor, value,
            SmartIslandSettings.Default.callColor)
    suspend fun setLiveActivityColor(value: Long) = putColor(SettingsKeys.LiveActivityColor, value,
            SmartIslandSettings.Default.liveActivityColor)
    suspend fun setTransferColor(value: Long) = putColor(SettingsKeys.TransferColor, value,
            SmartIslandSettings.Default.transferColor)
    suspend fun setNavigationColor(value: Long) = putColor(SettingsKeys.NavigationColor, value,
            SmartIslandSettings.Default.navigationColor)
    suspend fun setBluetoothColor(value: Long) = putColor(SettingsKeys.BluetoothColor, value,
            SmartIslandSettings.Default.bluetoothColor)
    suspend fun setFlashlightColor(value: Long) = putColor(SettingsKeys.FlashlightColor, value,
            SmartIslandSettings.Default.flashlightColor)
    suspend fun setScreenRecordingColor(value: Long) = putColor(SettingsKeys.ScreenRecordingColor, value,
            SmartIslandSettings.Default.screenRecordingColor)
    suspend fun setTimerColor(value: Long) = putColor(SettingsKeys.TimerColor, value,
            SmartIslandSettings.Default.timerColor)
    suspend fun setStopwatchColor(value: Long) = putColor(SettingsKeys.StopwatchColor, value,
            SmartIslandSettings.Default.stopwatchColor)
    /** Gives [eventId] a colour of its own; an unknown event or a zero colour changes nothing. */
    suspend fun setEventColor(eventId: String, value: Long) = editSafely {
        val next = EventColors.decode(it[SettingsKeys.EventColors]).toMutableMap()
        next[eventId] = value
        it[SettingsKeys.EventColors] = EventColors.encode(next)
    }

    /** Takes [eventId]'s own colour away, it goes back to the colour it always had. */
    suspend fun clearEventColor(eventId: String) = editSafely {
        val next = EventColors.decode(it[SettingsKeys.EventColors]) - eventId
        it[SettingsKeys.EventColors] = EventColors.encode(next)
    }

    /** Takes every event's own colour away. */
    suspend fun clearEventColors() = editSafely { it[SettingsKeys.EventColors] = emptySet() }

    suspend fun setEnableAppShortcuts(value: Boolean) = put(SettingsKeys.EnableAppShortcuts, value)
    suspend fun setShortcutPackages(value: List<String>) = editSafely {
        writePinnedApps(it, value)
    }
    suspend fun setShowRecentApps(value: Boolean) = put(SettingsKeys.ShowRecentApps, value)
    suspend fun setUtilityLayout(value: List<UtilityPlacement>) =
            putMapped(SettingsKeys.UtilityLayout, value) { UtilityLayout.encode(it) }
    suspend fun setUtilityCalculatorPackage(value: String) = editSafely {
        it[SettingsKeys.UtilityCalculatorPackage] = value.take(255)
    }
    suspend fun setUtilityCalculatorFloating(value: Boolean) = put(SettingsKeys.UtilityCalculatorFloating, value)
    suspend fun setWelcomeDialogShown(value: Boolean) = put(SettingsKeys.WelcomeDialogShown, value)

    suspend fun isWelcomeDialogShown(): Boolean = settings.first().welcomeDialogShown

    suspend fun setTorchStrobeAcknowledged(value: Boolean) = put(SettingsKeys.TorchStrobeAcknowledged, value)

    suspend fun setShowOnLockScreen(value: Boolean) = put(SettingsKeys.ShowOnLockScreen, value)
    suspend fun setEventShownInForeground(eventId: String, shown: Boolean) = editSafely {
        if (IslandEvent.FOREGROUND_SWITCHABLE.none { e -> e.id == eventId }) return@editSafely
        // the first edit starts from what is showing now, which may still come from the old single switch
        val current = it[SettingsKeys.ShowInForegroundEvents]
            ?: if (it[SettingsKeys.ShowLiveEventsInForeground] == true) {
                IslandEvent.FOREGROUND_SWITCHABLE.mapTo(LinkedHashSet()) { e -> e.id }
            } else {
                IslandEvent.DEFAULT_SHOW_IN_FOREGROUND_IDS
            }
        it[SettingsKeys.ShowInForegroundEvents] = if (shown) current + eventId else current - eventId
    }
    suspend fun setHideInFullscreen(value: Boolean) = put(SettingsKeys.HideInFullscreen, value)
    suspend fun setHideInShade(value: Boolean) = put(SettingsKeys.HideInShade, value)
    suspend fun setLockScreenPrivacy(value: String) = editSafely {
        it[SettingsKeys.LockScreenPrivacy] = value.takeIf { privacy ->
            privacy in VALID_LOCK_SCREEN_PRIVACY_VALUES
        } ?: SmartIslandSettings.Default.lockScreenPrivacy
    }
    suspend fun setShowNotificationActions(value: Boolean) = put(SettingsKeys.ShowNotificationActions, value)
    suspend fun setHideFromNotificationShade(value: Boolean) = put(SettingsKeys.HideFromNotificationShade, value)
    suspend fun setEventEnabled(eventId: String, enabled: Boolean) = editSafely {
        it.setMembership(SettingsKeys.DisabledEvents, eventId, !enabled)
    }

    suspend fun setHideWhenIdle(value: Boolean) = put(SettingsKeys.HideWhenIdle, value)

    suspend fun setAutoHidePill(value: Boolean) = put(SettingsKeys.AutoHidePill, value)
    suspend fun setAutoHideTimeoutSeconds(value: Int) = putClamped(SettingsKeys.AutoHideTimeoutSeconds, value,
            SmartIslandSettings.MIN_AUTO_HIDE_SECONDS, SmartIslandSettings.MAX_AUTO_HIDE_SECONDS)
    suspend fun setShowInLandscape(value: Boolean) = put(SettingsKeys.ShowInLandscape, value)
    suspend fun setAutoExpandOnNotification(value: Boolean) = put(SettingsKeys.AutoExpandOnNotification, value)
    suspend fun setShowSuggestedReplies(value: Boolean) = put(SettingsKeys.ShowSuggestedReplies, value)

    suspend fun setShowCopyCodeChip(value: Boolean) = put(SettingsKeys.ShowCopyCodeChip, value)
    suspend fun setAutoExpandOnLockScreen(value: Boolean) = put(SettingsKeys.AutoExpandOnLockScreen, value)
    suspend fun setIdleTimeoutNever(value: Boolean) = put(SettingsKeys.IdleTimeoutNever, value)
    suspend fun setIdleTimeoutMs(value: Int) = putClamped(SettingsKeys.IdleTimeoutMs, value,
            SmartIslandSettings.MIN_IDLE_TIMEOUT_MS, SmartIslandSettings.MAX_IDLE_TIMEOUT_MS)
    suspend fun setBatteryWarningMs(value: Int) = putClamped(SettingsKeys.BatteryWarningMs, value,
            SmartIslandSettings.MIN_BATTERY_WARNING_MS, SmartIslandSettings.MAX_BATTERY_WARNING_MS)
    suspend fun setPulseHoldPercent(value: Int) = putClamped(SettingsKeys.PulseHoldPercent, value,
            SmartIslandSettings.MIN_PULSE_HOLD_PERCENT, SmartIslandSettings.MAX_PULSE_HOLD_PERCENT)
    suspend fun setTransferExpiryMinutes(value: Int) = putClamped(SettingsKeys.TransferExpiryMinutes, value, 0,
            SmartIslandSettings.MAX_TRANSFER_EXPIRY_MINUTES)
    suspend fun setSecondaryMergeMs(value: Int) = putClamped(SettingsKeys.SecondaryMergeMs, value,
            SmartIslandSettings.MIN_SECONDARY_MERGE_MS, SmartIslandSettings.MAX_SECONDARY_MERGE_MS)
    suspend fun setAnimSpeedPortrait(value: String) = putChoice(SettingsKeys.AnimSpeedPortrait, value,
            AnimationSpeed.VALID_IDS, SmartIslandSettings.Default.animSpeedPortrait)
    suspend fun setAnimSpeedLandscape(value: String) = putChoice(SettingsKeys.AnimSpeedLandscape, value,
            AnimationSpeed.VALID_IDS, SmartIslandSettings.Default.animSpeedLandscape)
    suspend fun setMergeStyle(value: String) = putChoice(SettingsKeys.MergeStyleId, value, MergeStyle.VALID_IDS,
            SmartIslandSettings.Default.mergeStyle)
    suspend fun setMergeStyleLandscape(value: String) = putChoice(SettingsKeys.MergeStyleLandscapeId, value,
            MergeStyle.VALID_IDS, SmartIslandSettings.Default.mergeStyleLandscape)
    suspend fun setShowDuringDnd(value: Boolean) = put(SettingsKeys.ShowDuringDnd, value)
    suspend fun setHapticStrength(value: String) = putChoice(SettingsKeys.HapticStrength, value,
            HapticStrength.VALID_IDS, SmartIslandSettings.Default.hapticStrength)
    suspend fun setFloatingWindowEnabled(value: Boolean) = put(SettingsKeys.FloatingWindowEnabled, value)
    suspend fun setFloatingWindowMode(value: String) = putChoice(SettingsKeys.FloatingWindowMode, value,
            FloatingWindowMode.VALID_IDS, SmartIslandSettings.Default.floatingWindowMode)
    suspend fun setGestureSensitivity(value: String) = putChoice(SettingsKeys.GestureSensitivityId, value,
            GestureSensitivity.VALID_IDS, SmartIslandSettings.Default.gestureSensitivity)
    /** Sets one app's auto-expand choice in a single write. */
    suspend fun setAppAutoExpand(packageName: String, choice: AppAutoExpand) = editSafely {
        if (!validPackage(packageName)) return@editSafely
        val allow = (it[SettingsKeys.AutoExpandPackages] ?: emptySet()) - packageName
        val deny = (it[SettingsKeys.NoAutoExpandPackages] ?: emptySet()) - packageName
        when (choice) {
            AppAutoExpand.DEFAULT -> {
                it[SettingsKeys.AutoExpandPackages] = allow
                it[SettingsKeys.NoAutoExpandPackages] = deny
            }
            AppAutoExpand.ALWAYS -> {
                it[SettingsKeys.AutoExpandPackages] = allow + packageName
                it[SettingsKeys.NoAutoExpandPackages] = deny
            }
            AppAutoExpand.NEVER -> {
                it[SettingsKeys.AutoExpandPackages] = allow
                it[SettingsKeys.NoAutoExpandPackages] = deny + packageName
            }
        }
    }

    suspend fun setEventAutoExpand(eventId: String, enabled: Boolean) = editSafely {
        if (IslandEvent.entries.none { e -> e.id == eventId }) return@editSafely
        // The first edit turned auto-expand on for the other events in both layouts.
        // Start with the default exclusions if nothing is saved yet.
        val current = it[SettingsKeys.NoAutoExpandEvents] ?: IslandEvent.DEFAULT_NO_AUTO_EXPAND_IDS
        it[SettingsKeys.NoAutoExpandEvents] = if (enabled) current - eventId else current + eventId
    }

    suspend fun setLandscapeEventAutoExpand(eventId: String, enabled: Boolean) = editSafely {
        if (IslandEvent.entries.none { e -> e.id == eventId }) return@editSafely
        // The first landscape edit starts from the current portrait choices.
        val current = it[SettingsKeys.NoAutoExpandEventsLandscape] ?: it[SettingsKeys.NoAutoExpandEvents]
            ?: IslandEvent.DEFAULT_NO_AUTO_EXPAND_IDS
        it[SettingsKeys.NoAutoExpandEventsLandscape] = if (enabled) current - eventId else current + eventId
    }

    suspend fun setHideAllOnLockScreen(value: Boolean) = put(SettingsKeys.HideAllOnLockScreen, value)

    suspend fun setEventHiddenOnLockScreen(eventId: String, hidden: Boolean) = editSafely {
        if (IslandEvent.entries.none { e -> e.id == eventId && e.canHideOnLockScreen }) {
            return@editSafely
        }
        it.setMembership(SettingsKeys.HideOnLockScreenEvents, eventId, hidden)
    }

    suspend fun applyLockScreenPreset(preset: LockScreenPreset) = editSafely {
        when (preset) {
            LockScreenPreset.ShowAll -> {
                it[SettingsKeys.ShowOnLockScreen] = true
                it[SettingsKeys.HideAllOnLockScreen] = false
                it[SettingsKeys.HideOnLockScreenEvents] = emptySet()
            }
            LockScreenPreset.MessagesOnly -> {
                it[SettingsKeys.ShowOnLockScreen] = true
                it[SettingsKeys.HideAllOnLockScreen] = false
                it[SettingsKeys.HideOnLockScreenEvents] = IslandEvent.entries
                    .filter { e -> e.canHideOnLockScreen && e != IslandEvent.NOTIFICATION }
                    .map { e -> e.id }
                    .toSet()
            }
            LockScreenPreset.CallsOnly -> {
                it[SettingsKeys.ShowOnLockScreen] = true
                it[SettingsKeys.HideAllOnLockScreen] = true
                it[SettingsKeys.HideOnLockScreenEvents] = emptySet()
            }
            LockScreenPreset.Off -> it[SettingsKeys.ShowOnLockScreen] = false
        }
    }

    suspend fun setAppHiddenOnLockScreen(packageName: String, hidden: Boolean) =
        togglePackage(SettingsKeys.HideOnLockScreenPackages, packageName, hidden)

    private fun MutablePreferences.setMembership(key: Preferences.Key<Set<String>>, value: String, present: Boolean) {
        val current = this[key].orEmpty()
        this[key] = if (present) current + value else current - value
    }

    suspend fun setAppExcluded(packageName: String, excluded: Boolean) =
            togglePackage(SettingsKeys.DisabledNotificationPackages, packageName, excluded)

    suspend fun setAppMuted(packageName: String, muted: Boolean) =
            togglePackage(SettingsKeys.DisabledSoundPackages, packageName, muted)

    /** Whether the app's card may open into the big card (noCard = never). */
    suspend fun setAppNoCard(packageName: String, noCard: Boolean) =
            togglePackage(SettingsKeys.NoExpandPackages, packageName, noCard)

    suspend fun setAppCooldownBypass(packageName: String, bypass: Boolean) =
            togglePackage(SettingsKeys.NotificationCooldownExcludedPackages, packageName, bypass)

    suspend fun resetApps(packages: Collection<String>) = editSafely { prefs ->
        val targets = packages.filter { pkg -> validPackage(pkg) }.toSet()
        if (targets.isEmpty()) return@editSafely
        listOf(
            SettingsKeys.DisabledNotificationPackages,
            SettingsKeys.DisabledSoundPackages,
            SettingsKeys.NotificationCooldownExcludedPackages,
            SettingsKeys.NoExpandPackages,
            SettingsKeys.NoAutoExpandPackages,
            SettingsKeys.AutoExpandPackages,
            SettingsKeys.HideOnLockScreenPackages
        ).forEach { key -> prefs[key] = (prefs[key] ?: emptySet()) - targets }
    }

    suspend fun applyToApps(packages: Collection<String>, action: BulkAppAction) {
        when (action) {
            BulkAppAction.Reset -> resetApps(packages)
            BulkAppAction.Mute -> editSafely { prefs ->
                val targets = packages.filter { pkg -> validPackage(pkg) }
                prefs[SettingsKeys.DisabledSoundPackages] = (prefs[SettingsKeys.DisabledSoundPackages]
                    ?: emptySet()) + targets
            }
            BulkAppAction.Exclude -> editSafely { prefs ->
                val targets = packages.filter { pkg -> validPackage(pkg) }
                prefs[SettingsKeys.DisabledNotificationPackages] =
                    (prefs[SettingsKeys.DisabledNotificationPackages] ?: emptySet()) + targets
            }
        }
    }

    suspend fun setNotificationsEnabled(value: Boolean) = put(SettingsKeys.NotificationsEnabled, value)

    suspend fun resetTimings() = editSafely {
        val d = SmartIslandSettings.Default
        it[SettingsKeys.IdleTimeoutMs] = d.idleTimeoutMs
        it[SettingsKeys.IdleTimeoutNever] = d.idleTimeoutNever
        it[SettingsKeys.AutoHidePill] = d.autoHidePill
        it[SettingsKeys.AutoHideTimeoutSeconds] = d.autoHideTimeoutSeconds
        it[SettingsKeys.BatteryWarningMs] = d.batteryWarningMs
        it[SettingsKeys.PulseHoldPercent] = d.pulseHoldPercent
        it[SettingsKeys.SecondaryMergeMs] = d.secondaryMergeMs
        it[SettingsKeys.TransferExpiryMinutes] = d.transferExpiryMinutes
    }
    suspend fun setIdleWidgetsEnabled(value: Boolean) = put(SettingsKeys.IdleWidgetsEnabled, value)
    suspend fun setIdleWidgetsEnabledLandscape(value: Boolean) = put(SettingsKeys.IdleWidgetsEnabledLandscape, value)
    suspend fun setIdleOffAfterTapClose(landscape: Boolean, value: Boolean) = editSafely {
        it[if (landscape) SettingsKeys.IdleOffAfterTapCloseLandscape else SettingsKeys.IdleOffAfterTapClose] = value
    }
    suspend fun setIslandExemptApp(landscape: Boolean, packageName: String, exempt: Boolean) = editSafely {
        if (!validPackage(packageName)) return@editSafely
        val key = if (landscape) SettingsKeys.IslandExemptAppsLandscape else SettingsKeys.IslandExemptApps
        it.setMembership(key, packageName, exempt)
        // portrait: an app is either "off" or "static", never both
        if (!landscape) it[SettingsKeys.IslandExemptAppsStatic] =
            (it[SettingsKeys.IslandExemptAppsStatic] ?: emptySet()) - packageName
    }

    suspend fun setIslandExemptStatic(packageName: String, static: Boolean) = editSafely {
        if (!validPackage(packageName)) return@editSafely
        val off = it[SettingsKeys.IslandExemptApps] ?: emptySet()
        val stat = it[SettingsKeys.IslandExemptAppsStatic] ?: emptySet()
        if (static) {
            it[SettingsKeys.IslandExemptApps] = off - packageName
            it[SettingsKeys.IslandExemptAppsStatic] = stat + packageName
        } else {
            it[SettingsKeys.IslandExemptAppsStatic] = stat - packageName
            it[SettingsKeys.IslandExemptApps] = off + packageName
        }
    }
    suspend fun setContentPillSameAsIdle(landscape: Boolean, value: Boolean) = editSafely {
        it[if (landscape) SettingsKeys.ContentPillSameAsIdleLandscape else SettingsKeys.ContentPillSameAsIdle] = value
    }
    suspend fun setContentPillWidthPercent(landscape: Boolean, value: Float) = editSafely {
        it[if (landscape) SettingsKeys.ContentPillWidthPercentLandscape else SettingsKeys.ContentPillWidthPercent] =
            if (value.isNaN()) SmartIslandSettings.DEFAULT_CONTENT_PILL_PERCENT
        else value.coerceIn(SmartIslandSettings.MIN_CONTENT_PILL_PERCENT, SmartIslandSettings.MAX_CONTENT_PILL_PERCENT)
    }
    suspend fun setIdleOffWhenStored(landscape: Boolean, value: Boolean) = editSafely {
        it[if (landscape) SettingsKeys.IdleOffWhenStoredLandscape else SettingsKeys.IdleOffWhenStored] = value
    }
    suspend fun setIdleOffAfterLongPress(landscape: Boolean, value: Boolean) = editSafely {
        it[if (landscape) SettingsKeys.IdleOffAfterLongPressLandscape else SettingsKeys.IdleOffAfterLongPress] = value
    }
    suspend fun setIdleGlyphTickSeconds(value: Float) = putMapped(SettingsKeys.IdleGlyphTickSeconds,
            value) { IdleSlots.normalizeTickSeconds(it) }
    suspend fun setIdleWidgetIds(value: String) = putMapped(SettingsKeys.IdleWidgetIds,
            value) { IdleWidgets.normalize(it) }
    suspend fun setIdleWidgetCycleSeconds(value: Int) = putClamped(SettingsKeys.IdleWidgetCycleSeconds, value,
            IdleWidgets.MIN_CYCLE_SECONDS, IdleWidgets.MAX_CYCLE_SECONDS)
    suspend fun setIdleAlarmHideBeyond12h(value: Boolean) = put(SettingsKeys.IdleAlarmHideBeyond12h, value)
    suspend fun setBatteryOnlyWhenCharging(value: Boolean) = put(SettingsKeys.BatteryOnlyWhenCharging, value)
    suspend fun setIdleBeatOnlyOnChange(value: Boolean) = put(SettingsKeys.IdleBeatOnlyOnChange, value)
    suspend fun setWidgetIslandCloseSeconds(value: Int) = putMapped(SettingsKeys.WidgetIslandCloseSeconds,
            value) { WidgetIslandTimer.normalize(it) }
    suspend fun setWidgetIslandBatteryMetric(value: String) = editSafely {
        it[SettingsKeys.WidgetIslandBatteryMetric] = value.takeIf { v ->
            v in WidgetIslandContent.VALID_BATTERY_METRICS } ?: WidgetIslandContent.BATTERY_WATT
    }
    /** Mix fonts: the switch and the 3 slot fonts in 1 write. */
    suspend fun setFontMix(enabled: Boolean, light: String, regular: String, medium: String) = editSafely {
        fun valid(id: String) = id.takeIf { v -> v.isEmpty() || SmartIslandSettings.isValidFont(v) } ?: ""
        it[SettingsKeys.FontMixEnabled] = enabled
        it[SettingsKeys.FontMixLight] = valid(light)
        it[SettingsKeys.FontMixRegular] = valid(regular)
        it[SettingsKeys.FontMixMedium] = valid(medium)
    }
    suspend fun setIdleGlyphFont(value: String) = editSafely {
        it[SettingsKeys.IdleGlyphFont] = value.takeIf { v -> v.isEmpty() || SmartIslandSettings.isValidFont(v) } ?: ""
    }
    suspend fun setIdleGlyphScale(value: Int) = putMapped(SettingsKeys.IdleGlyphScale,
            value) { IdleSlots.normalizeGlyphScale(it) }
    suspend fun setIdleTickStyle(value: String) = putMapped(SettingsKeys.IdleTickStyle,
            value) { LedTick.normalizeStyle(it) }
    suspend fun setLedTickPattern(value: String) = putMapped(SettingsKeys.LedTickPattern,
            value) { LedTick.normalizePattern(it) }
    suspend fun setLedTickPaceMs(value: Int) = putMapped(SettingsKeys.LedTickPaceMs,
            value) { LedTick.normalizePace(it) }
    suspend fun setLedTickFont(value: String) = editSafely {
        it[SettingsKeys.LedTickFont] = value.takeIf { v -> v.isEmpty() || SmartIslandSettings.isValidFont(v) } ?: ""
    }
    suspend fun setLedTickColor(value: String) = putMapped(SettingsKeys.LedTickColor,
            value) { LedTick.normalizeColor(it) }
    suspend fun setLedTickStagger(enabled: Boolean) = put(SettingsKeys.LedTickStagger, enabled)
    suspend fun setIdleWidgetTempUnit(value: String) = editSafely {
        it[SettingsKeys.IdleWidgetTempUnit] = value.takeIf { v -> v in IdleWidgets.VALID_UNITS }
            ?: IdleWidgets.UNIT_AUTO
    }
    suspend fun setIdleWidgetWindUnit(value: String) = editSafely {
        it[SettingsKeys.IdleWidgetWindUnit] = value.takeIf { v -> v in IdleWidgets.VALID_WIND_UNITS }
            ?: IdleWidgets.UNIT_AUTO
    }
    suspend fun setWeatherLocationSource(value: String) = putMapped(SettingsKeys.WeatherLocationSource,
            value) { WeatherConfig.normalizeSource(it) }
    suspend fun setWeatherManualCity(name: String, lat: Float, lon: Float) = editSafely {
        if (WeatherConfig.validCoordinates(lat, lon)) {
            it[SettingsKeys.WeatherManualCity] = WeatherConfig.normalizeCity(name)
            it[SettingsKeys.WeatherManualLat] = lat
            it[SettingsKeys.WeatherManualLon] = lon
        }
    }
    suspend fun setWeatherRefreshMinutes(value: Int) = putMapped(SettingsKeys.WeatherRefreshMinutes,
            value) { WeatherConfig.normalizeRefreshMinutes(it) }
    suspend fun setWeatherProvider(value: String) = putMapped(SettingsKeys.WeatherProvider,
            value) { WeatherConfig.normalizeProvider(it) }
    suspend fun setWeatherCustomUrl(value: String) = putMapped(SettingsKeys.WeatherCustomUrl,
            value) { WeatherConfig.normalizeCustomUrl(it) }
    suspend fun setWeatherCustomKey(value: String) = putMapped(SettingsKeys.WeatherCustomKey,
            value) { WeatherConfig.normalizeKey(it) }
    suspend fun setWeatherPillMode(value: String) = putMapped(SettingsKeys.WeatherPillMode,
            value) { WeatherPill.normalize(it) }
    suspend fun setWeatherIslandReadouts(value: String) = putMapped(SettingsKeys.WeatherIslandReadouts,
            value) { WeatherReadouts.normalize(it) }
    suspend fun setWeatherIconAnimated(value: Boolean) = put(SettingsKeys.WeatherIconAnimated, value)
    suspend fun setWeatherIconPauseOnSaver(value: Boolean) = put(SettingsKeys.WeatherIconPauseOnSaver, value)
    suspend fun setWeatherShowRule(value: String) = putMapped(SettingsKeys.WeatherShowRule,
            value) { WeatherRules.normalizeMode(it) }
    suspend fun setWeatherNotableTriggers(value: String) = putMapped(SettingsKeys.WeatherNotableTriggers,
            value) { WeatherRules.normalizeTriggers(it) }
    suspend fun setWeatherHotC(value: Int) = putMapped(SettingsKeys.WeatherHotC,
            value) { WeatherRules.normalizeHotC(it) }
    suspend fun setWeatherColdC(value: Int) = putMapped(SettingsKeys.WeatherColdC,
            value) { WeatherRules.normalizeColdC(it) }
    suspend fun setWeatherHideAtNight(value: Boolean) = put(SettingsKeys.WeatherHideAtNight, value)
    suspend fun setWeatherNightStartHour(value: Int) = putMapped(SettingsKeys.WeatherNightStartHour,
            value) { WeatherRules.normalizeHour(it) }
    suspend fun setWeatherNightEndHour(value: Int) = putMapped(SettingsKeys.WeatherNightEndHour,
            value) { WeatherRules.normalizeHour(it) }
    suspend fun setNetSpeedConnection(value: String) = putMapped(SettingsKeys.NetSpeedConnection,
            value) { NetSpeedRules.normalizeConnection(it) }
    suspend fun setIdleScheduleWorkEnabled(value: Boolean) = put(SettingsKeys.IdleScheduleWorkEnabled, value)
    suspend fun setIdleScheduleWorkHidden(value: String) = putMapped(SettingsKeys.IdleScheduleWorkHidden,
            value) { IdleSchedule.normalizeHidden(it) }
    suspend fun setIdleScheduleWorkStartHour(value: Int) = putMapped(SettingsKeys.IdleScheduleWorkStartHour,
            value) { IdleSchedule.normalizeHour(it) }
    suspend fun setIdleScheduleWorkEndHour(value: Int) = putMapped(SettingsKeys.IdleScheduleWorkEndHour,
            value) { IdleSchedule.normalizeHour(it) }
    suspend fun setIdleScheduleWorkWeekdaysOnly(value: Boolean) = put(SettingsKeys.IdleScheduleWorkWeekdaysOnly, value)
    suspend fun setIdleScheduleNightEnabled(value: Boolean) = put(SettingsKeys.IdleScheduleNightEnabled, value)
    suspend fun setIdleScheduleNightHidden(value: String) = putMapped(SettingsKeys.IdleScheduleNightHidden,
            value) { IdleSchedule.normalizeHidden(it) }
    suspend fun setIdleScheduleNightStartHour(value: Int) = putMapped(SettingsKeys.IdleScheduleNightStartHour,
            value) { IdleSchedule.normalizeHour(it) }
    suspend fun setIdleScheduleNightEndHour(value: Int) = putMapped(SettingsKeys.IdleScheduleNightEndHour,
            value) { IdleSchedule.normalizeHour(it) }
    suspend fun setIdleScheduleChargingEnabled(value: Boolean) = put(SettingsKeys.IdleScheduleChargingEnabled, value)
    suspend fun setIdleScheduleChargingHidden(value: String) =
            putMapped(SettingsKeys.IdleScheduleChargingHidden, value) { IdleSchedule.normalizeHidden(it) }
    /** Sets the widget order and the pinned widget together. */
    suspend fun setIdleWidgetOrder(ids: String, pinnedFirst: String) = editSafely {
        it[SettingsKeys.IdleWidgetIds] = IdleWidgets.normalize(ids)
        it[SettingsKeys.IdleWidgetPinnedFirst] = IdleSchedule.normalizePinned(pinnedFirst)
    }
    suspend fun setNetSpeedThresholdKbps(value: Int) = putMapped(SettingsKeys.NetSpeedThresholdKbps,
            value) { NetSpeedRules.normalize(it) }
    suspend fun setAllowNetworkChecks(value: Boolean) = put(SettingsKeys.AllowNetworkChecks, value)
    suspend fun setDetectTransfersBySpeed(value: Boolean) = put(SettingsKeys.DetectTransfersBySpeed, value)
    suspend fun setEnableNotificationHistory(value: Boolean) = put(SettingsKeys.EnableNotificationHistory, value)
    suspend fun setNotificationHistoryRetentionHours(value: Int) =
            put(SettingsKeys.NotificationHistoryRetentionHours, value)
    suspend fun setShowBluetoothBattery(value: Boolean) = put(SettingsKeys.ShowBluetoothBattery, value)
    suspend fun setEnableNotificationCooldown(value: Boolean) = put(SettingsKeys.EnableNotificationCooldown, value)
    suspend fun setNotificationCooldownDurationMinutes(value: Int) =
            putClamped(SettingsKeys.NotificationCooldownDurationMinutes, value, 1, 60)
    suspend fun setNotificationCooldownThreshold(value: Int) =
            putClamped(SettingsKeys.NotificationCooldownThreshold, value, 2, 20)
    suspend fun setNotificationCooldownExcludedPackages(packages: Set<String>) =
            put(SettingsKeys.NotificationCooldownExcludedPackages, packages)
    suspend fun setThemeMode(value: String) = put(SettingsKeys.ThemeMode, value)
    suspend fun setLiquidGlassTheme(value: Boolean) = put(SettingsKeys.LiquidGlassTheme, value)
    suspend fun setMaterialYou(value: Boolean) = put(SettingsKeys.MaterialYou, value)
    suspend fun setAccentColor(value: Long) = putColor(SettingsKeys.AccentColor, value,
            SmartIslandSettings.Default.accentColor)
    suspend fun setExpandedTheme(value: String) = putChoice(SettingsKeys.ExpandedTheme, value,
            SmartIslandSettings.VALID_EXPANDED_THEMES, SmartIslandSettings.Default.expandedTheme)
    suspend fun setIslandFont(value: String) = editSafely {
        it[SettingsKeys.IslandFont] =
            if (SmartIslandSettings.isValidFont(value)) value else SmartIslandSettings.Default.islandFont
    }
    suspend fun setFaceUnlockMode(value: String) = editSafely {
        it[SettingsKeys.FaceUnlockMode] = if (value == "any") "any" else "face_only"
    }
    suspend fun setFaceIdStyle(value: String) = editSafely {
        it[SettingsKeys.FaceIdStyle] = if (value == "cutout" || value == "square_tick") value else "square"
    }
    suspend fun setStatusBarHidden(value: Boolean) = put(SettingsKeys.StatusBarHidden, value)
    suspend fun setStatusBarHidePersistent(value: Boolean) = put(SettingsKeys.StatusBarHidePersistent, value)
    suspend fun setStatusBarHideNotificationIcons(value: Boolean) =
            put(SettingsKeys.StatusBarHideNotificationIcons, value)
    suspend fun setStatusBarHideClock(value: Boolean) = put(SettingsKeys.StatusBarHideClock, value)
    suspend fun setStatusBarHideAllSystemIcons(value: Boolean) = put(SettingsKeys.StatusBarHideAllSystemIcons, value)
    suspend fun setStatusBarHiddenSystemIcons(keys: Set<String>) = put(SettingsKeys.StatusBarHiddenSystemIcons, keys)
    suspend fun setStatusBarSystemIconHidden(key: String, hidden: Boolean) = editSafely {
        it.setMembership(SettingsKeys.StatusBarHiddenSystemIcons, key, hidden)
    }
    suspend fun setAppFaceIdEnabled(value: Boolean) = put(SettingsKeys.AppFaceIdEnabled, value)
    suspend fun setFingerprintAnimationEnabled(value: Boolean) = put(SettingsKeys.FingerprintAnimationEnabled, value)
    suspend fun setAppFaceIdStyle(value: String) = editSafely {
        it[SettingsKeys.AppFaceIdStyle] = if (value == "cutout" || value == "square_tick") value else "square"
    }
    suspend fun setRecordLogs(value: Boolean) = put(SettingsKeys.RecordLogs, value)
    suspend fun switchLayoutMode(toNotch: Boolean, compactFallback: CutoutFitPlan? = null) = editSafely {
        SettingsLayoutWriter.switchLayoutMode(it, toNotch, compactFallback, teardropPhone)
    }
    suspend fun setCameraCutoutManual(value: Boolean) = editSafely {
        it[SettingsKeys.CameraCutoutManual] = value
        fitEdgePillToPlacedCutout(it)
    }
    suspend fun setCameraCutoutStyle(style: String) = editSafely {
        val valid = style.takeIf { s -> s in VALID_CUTOUT_STYLES } ?: SmartIslandSettings.Default.cameraCutoutStyle
        if (it[SettingsKeys.CameraCutoutStyle] != valid) {
            it[SettingsKeys.CameraCutoutStyle] = valid
            it[SettingsKeys.CameraCutoutX] = SmartIslandSettings.Default.cameraCutoutX
        }
        fitEdgePillToPlacedCutout(it)
    }

    /**
     * This keeps the landscape edge pill fitted when the portrait cutout is moved by hand or set by a preset.
     * A manual landscape camera takes precedence.
     */
    private fun fitEdgePillToPlacedCutout(prefs: MutablePreferences) {
        val cfg = context.resources.configuration
        val portraitWidthDp = minOf(cfg.screenWidthDp, cfg.screenHeightDp).toFloat()
        if (portraitWidthDp <= 0f) return
        val hole = CutoutPlacement.placedFor(readSettings(prefs, teardropPhone), portraitWidthDp)
            ?.clampedTo(portraitWidthDp) ?: return
        val edge = EdgePillTuning.decode(prefs[SettingsKeys.EdgePill], flatEdgeDefault = teardropPhone)
        if (edge.cameraManual) return
        // a hole placed by hand is no measure of the camera, so the pill keeps at least its default size
        prefs[SettingsKeys.EdgePill] = EdgePillFit.fitPortrait(edge, hole, assumed = true).coerced().encode()
    }
    suspend fun setCameraCutoutArea(x: Float, y: Float, width: Float, height: Float) = editSafely {
        val d = SmartIslandSettings.Default
        it[SettingsKeys.CameraCutoutX] = validDimension(x, d.cameraCutoutX, SmartIslandSettings.MIN_CUTOUT_X,
            SmartIslandSettings.MAX_CUTOUT_X)
        it[SettingsKeys.CameraCutoutY] = validDimension(y, d.cameraCutoutY, SmartIslandSettings.MIN_Y_OFFSET,
            SmartIslandSettings.MAX_Y_OFFSET)
        it[SettingsKeys.CameraCutoutWidth] = validDimension(width, d.cameraCutoutWidth,
            SmartIslandSettings.MIN_CUTOUT_WIDTH, SmartIslandSettings.MAX_CUTOUT_WIDTH)
        it[SettingsKeys.CameraCutoutHeight] = validDimension(height, d.cameraCutoutHeight,
            SmartIslandSettings.MIN_CUTOUT_HEIGHT, SmartIslandSettings.MAX_CUTOUT_HEIGHT)
        fitEdgePillToPlacedCutout(it)
    }
    suspend fun isCutoutFitDone(): Boolean =
        runSuspendCatching { context.smartIslandDataStore.data.first()[SettingsKeys.CutoutFitDone] == true }
            .getOrDefault(true)

    suspend fun markCutoutFitDone() = editSafely { it[SettingsKeys.CutoutFitDone] = true }

    suspend fun isNoCutoutPromptDone(): Boolean =
        runSuspendCatching { context.smartIslandDataStore.data.first()[SettingsKeys.NoCutoutPromptDone] == true }
            .getOrDefault(true)

    suspend fun markNoCutoutPromptDone() = editSafely { it[SettingsKeys.NoCutoutPromptDone] = true }

    val listenerSettings: Flow<SmartIslandSettings> by lazy {
      combine(
        listOf(
            settings,
            forPosture(FoldPosture.FoldedPortrait).settings,
            forPosture(FoldPosture.UnfoldedPortrait).settings,
            forPosture(FoldPosture.FoldedLandscape).settings,
            forPosture(FoldPosture.UnfoldedLandscape).settings
        )
    ) { values ->
        val base = values[0] as SmartIslandSettings
        val portrait = listOf(values[1], values[2]).map { it as SmartIslandSettings }
        val landscape = listOf(values[3], values[4]).map { it as SmartIslandSettings }
        base.copy(
            disabledEvents = portrait.map { it.disabledEvents }.reduce { a, b -> a intersect b },
            disabledEventsLandscape = landscape.map { it.disabledEventsLandscape }.reduce { a, b -> a intersect b },
            showInLandscape = landscape.any { it.showInLandscape },
            foldDisabledSomewhere = portrait.flatMap { it.disabledEvents }.toSet() +
                landscape.filter { it.showInLandscape }.flatMap { it.disabledEventsLandscape }
        )
      }
    }

    /** Writes a detected-hole layout in one edit. */
    suspend fun applyCutoutFit(
        plan: CutoutFitPlan,
        edgeHole: CutoutArea? = null,
        teardrop: Boolean? = null
    ) = editSafely {
        SettingsLayoutWriter.applyCutoutFit(it, plan, edgeHole, teardrop, teardropPhone)
    }

    suspend fun setEnableSwipeActions(value: Boolean) = put(SettingsKeys.EnableSwipeActions, value)
    suspend fun setSwipeUpAction(value: String) = put(SettingsKeys.SwipeUpAction, value)
    suspend fun setSwipeHoldUpAction(value: String) = put(SettingsKeys.SwipeHoldUpAction, value)
    suspend fun setSwipeDownAction(value: String) = put(SettingsKeys.SwipeDownAction, value)
    suspend fun setSwipeLeftAction(value: String) = put(SettingsKeys.SwipeLeftAction, value)
    suspend fun setSwipeRightAction(value: String) = put(SettingsKeys.SwipeRightAction, value)
    suspend fun setEnablePillSwipeActions(value: Boolean) = put(SettingsKeys.EnablePillSwipeActions, value)
    suspend fun setPillSwipeUpAction(value: String) = put(SettingsKeys.PillSwipeUpAction, value)
    suspend fun setLongPressAction(value: String) = put(SettingsKeys.LongPressAction, value)
    suspend fun setPillSwipeDownAction(value: String) = put(SettingsKeys.PillSwipeDownAction, value)
    suspend fun setPillSwipeLeftAction(value: String) = put(SettingsKeys.PillSwipeLeftAction, value)
    suspend fun setPillSwipeRightAction(value: String) = put(SettingsKeys.PillSwipeRightAction, value)
    suspend fun setLandscapeEventEnabled(eventId: String, enabled: Boolean) = editSafely {
        it.setMembership(SettingsKeys.DisabledEventsLandscape, eventId, !enabled)
    }
    suspend fun setEdgePill(value: EdgePillTuning) = editSafely {
        it[SettingsKeys.EdgePill] = value.coerced().encode()
    }
    suspend fun switchEdgeLook(
        current: EdgePillTuning,
        toFlat: Boolean,
        firstVisit: EdgePillTuning? = null
    ): EdgePillTuning {
        var result = current.copy(flatEdge = toFlat)
        editSafely { result = SettingsLayoutWriter.switchEdgeLook(it, current, toFlat, firstVisit) }
        return result
    }
    suspend fun setLandscapeLayoutStyle(value: String) = putMapped(SettingsKeys.LandscapeLayoutStyle,
            value) { LandscapeLayoutStyle.fromId(it) }
    suspend fun setLandscapePillPosition(value: String) = putChoice(SettingsKeys.LandscapePillPosition, value,
            VALID_LANDSCAPE_POSITIONS, SmartIslandSettings.Default.landscapePillPosition)
    suspend fun setLandscapePillGeometry(
        width: Float,
        height: Float,
        xOffset: Float,
        yOffset: Float
    ) = editSafely {
        val d = SmartIslandSettings.Default
        it[SettingsKeys.LandscapeWidth] = validDimension(width, d.landscapeWidth, SmartIslandSettings.MIN_WIDTH,
            SmartIslandSettings.MAX_WIDTH)
        it[SettingsKeys.LandscapeHeight] = validDimension(height, d.landscapeHeight, SmartIslandSettings.MIN_HEIGHT,
            SmartIslandSettings.MAX_HEIGHT)
        it[SettingsKeys.LandscapeXOffset] = validDimension(xOffset, d.landscapeXOffset,
            -SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET)
        it[SettingsKeys.LandscapeYOffset] = validDimension(yOffset, d.landscapeYOffset,
            SmartIslandSettings.MIN_Y_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET)
    }
    suspend fun setLandscapeOpacity(value: Float) = putDimension(SettingsKeys.LandscapeOpacity, value,
            SmartIslandSettings.Default.landscapeOpacity, SmartIslandSettings.MIN_OPACITY,
            SmartIslandSettings.MAX_OPACITY)
    suspend fun setLandscapeCircleSize(value: Float) = putDimension(SettingsKeys.LandscapeCircleSize, value,
            SmartIslandSettings.Default.landscapeCircleSize, SmartIslandSettings.MIN_CIRCLE_SIZE,
            SmartIslandSettings.MAX_CIRCLE_SIZE)
    suspend fun setLandscapeCircleOpacity(value: Float) = putDimension(SettingsKeys.LandscapeCircleOpacity, value,
            SmartIslandSettings.Default.landscapeCircleOpacity, SmartIslandSettings.MIN_CIRCLE_OPACITY,
            SmartIslandSettings.MAX_OPACITY)
    suspend fun setLandscapeCircleEdgeInset(value: Float) = putDimension(SettingsKeys.LandscapeCircleEdgeInset, value,
            SmartIslandSettings.Default.landscapeCircleEdgeInset, SmartIslandSettings.MIN_CIRCLE_EDGE_INSET,
            SmartIslandSettings.MAX_CIRCLE_EDGE_INSET)
    suspend fun setLandscapeCircleXAdjust(value: Float) = putDimension(SettingsKeys.LandscapeCircleXAdjust, value,
            SmartIslandSettings.Default.landscapeCircleXAdjust, -SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET,
            SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET)
    suspend fun setLandscapeCircleYAdjust(value: Float) = putDimension(SettingsKeys.LandscapeCircleYAdjust, value,
            SmartIslandSettings.Default.landscapeCircleYAdjust, -SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET,
            SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET)
    suspend fun setCirclePosition(value: String) = putChoice(SettingsKeys.CirclePosition, value, VALID_CIRCLE_POSITIONS,
            SmartIslandSettings.CIRCLE_POSITION_RIGHT)
    suspend fun setNotificationCooldownSystemEvents(eventIds: Set<String>) =
            put(SettingsKeys.NotificationCooldownSystemEvents, eventIds)
    suspend fun toggleNotificationCooldownSystemEvent(eventId: String) = editSafely { prefs ->
        val current = prefs[SettingsKeys.NotificationCooldownSystemEvents] ?: emptySet()
        prefs[SettingsKeys.NotificationCooldownSystemEvents] =
            if (eventId in current) current - eventId else current + eventId
    }
    suspend fun toggleNotificationCooldownExcludedPackage(packageName: String) = editSafely { prefs ->
        val current = prefs[SettingsKeys.NotificationCooldownExcludedPackages] ?: emptySet()
        prefs[SettingsKeys.NotificationCooldownExcludedPackages] = if (packageName in current) {
            current - packageName
        } else {
            current + packageName
        }
    }

    suspend fun restoreSettings(settings: SmartIslandSettings) = editSafely { prefs ->
        writeSettings(prefs, settings)
    }

    suspend fun resetAllSettings() {
        val base = SmartIslandSettings.Default
        restoreSettings(
            base.copy(
                welcomeDialogShown = true,
                enableNotchMode = teardropPhone,
                edgePill = base.edgePill.copy(flatEdge = teardropPhone)
            )
        )
        // a restore leaves the weather API key alone (backups never hold it)
        setWeatherCustomKey("")
        // The saved per-mode layouts go with the rest of the settings
        editSafely {
            it.remove(SettingsKeys.SavedCompactLayout); it.remove(SettingsKeys.SavedNotchLayout)
            it.remove(SettingsKeys.SavedEdgeFlatLook); it.remove(SettingsKeys.SavedEdgeRoundLook)
            // Every fold mode goes back to following the normal settings
            FoldProfiles.clearAll(it)
        }
    }

    private suspend fun editSafely(transform: suspend (MutablePreferences) -> Unit) {
        try {
            val mode = activeProfile
            if (mode == null) {
                context.smartIslandDataStore.edit(transform)
            } else {
                context.smartIslandDataStore.edit { raw -> FoldProfiles.edit(raw, mode, transform) }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppLogRecorder.e(TAG, "could not persist settings", error)
        }
    }

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) = editSafely { it[key] = value }

    private suspend fun <T, R> putMapped(key: Preferences.Key<R>, value: T, map: (T) -> R) =
        editSafely { it[key] = map(value) }

    private suspend fun <T : Comparable<T>> putClamped(key: Preferences.Key<T>, value: T, min: T, max: T) =
        editSafely { it[key] = value.coerceIn(min, max) }

    private suspend fun putDimension(key: Preferences.Key<Float>, value: Float?, fallback: Float, min: Float,
            max: Float) =
        editSafely { it[key] = validDimension(value, fallback, min, max) }

    private suspend fun putColor(key: Preferences.Key<Long>, value: Long?, fallback: Long) =
        editSafely { it[key] = validColor(value, fallback) }

    private suspend fun putChoice(key: Preferences.Key<String>, value: String, valid: Set<String>, fallback: String) =
        editSafely { it[key] = if (value in valid) value else fallback }

    private suspend fun togglePackage(key: Preferences.Key<Set<String>>, packageName: String, present: Boolean) =
        editSafely {
            if (!validPackage(packageName)) return@editSafely
            it.setMembership(key, packageName, present)
        }

    private companion object {
        const val TAG = "SmartIslandSettings"
    }
}
