/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IdleSchedule
import com.agupta07505.smartisland.model.IdleSlots
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.IdleWidgetOff
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.ui.SettingsCard
import com.agupta07505.smartisland.ui.SettingsDescription
import com.agupta07505.smartisland.ui.glass.Switch
import com.agupta07505.smartisland.ui.search.searchAnchor
import com.agupta07505.smartisland.util.DndModeReader
import com.agupta07505.smartisland.util.runCatchingLogged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun IdleWidgetsSettingsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    onOpenWeatherSettings: () -> Unit = {},
    onOpenNetSpeedSettings: () -> Unit = {},
    onOpenCalendarSettings: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(LayoutMode.Portrait) }
    val editing = rememberEditingProfile(repository, settings, tab)
    val landscape = editing.mode.landscape
    val modeSettings = editing.settings
    val modeRepository = editing.repository
    var showSelector by rememberSaveable { mutableStateOf(false) }

    val enabled = if (landscape) modeSettings.idleWidgetsEnabledLandscape else modeSettings.idleWidgetsEnabled
    val offAfterTap = if (landscape) modeSettings.idleOffAfterTapCloseLandscape else modeSettings.idleOffAfterTapClose
    val offAfterLongPress =
        if (landscape) modeSettings.idleOffAfterLongPressLandscape else modeSettings.idleOffAfterLongPress
    val offWhenStored = if (landscape) modeSettings.idleOffWhenStoredLandscape else modeSettings.idleOffWhenStored
    val selected = remember(settings.idleWidgetIds, settings.idleWidgetPinnedFirst) {
        IdleSchedule.ordered(IdleWidgets.parse(settings.idleWidgetIds), settings.idleWidgetPinnedFirst)
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var dndAccess by remember { mutableStateOf(DndModeReader.hasPolicyAccess(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) dndAccess = DndModeReader.hasPolicyAccess(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .searchAnchor("idle.screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LayoutModeTabs(selected = editing.mode, foldableMode = editing.foldableMode, onSelected = { tab = it })

        IdleMasterCard(
            enabled = enabled,
            offAfterTap = offAfterTap,
            offAfterLongPress = offAfterLongPress,
            offWhenStored = offWhenStored,
            landscape = landscape,
            modeRepository = modeRepository,
            scope = scope
        )

        IdleSelectCard(
            settings = settings,
            repository = repository,
            scope = scope,
            context = context,
            selected = selected,
            dndAccess = dndAccess,
            onOpenWeatherSettings = onOpenWeatherSettings,
            onOpenNetSpeedSettings = onOpenNetSpeedSettings,
            onOpenCalendarSettings = onOpenCalendarSettings,
            onShowSelector = { showSelector = true }
        )

        IdleScheduleCard(settings = settings, repository = repository, selected = selected,
            modifier = Modifier.searchAnchor("idle.schedule"))

        IdleTimersCard(settings = settings, repository = repository, scope = scope)

        IdleUnitsCard(settings = settings, repository = repository)

        // weather, Calendar and Network speed settings live on their own pages

        LedTickSettingsCard(settings = settings, repository = repository)
    }
    if (showSelector) {
        WidgetSelectorDialog(
            onOpenWeather = onOpenWeatherSettings,
            onOpenNetSpeed = onOpenNetSpeedSettings,
            storedIds = settings.idleWidgetIds,
            pinnedFirst = settings.idleWidgetPinnedFirst,
            onChange = { ids -> scope.launch { repository.setIdleWidgetIds(ids) } },
            onOrder = { ids, pinned -> scope.launch { repository.setIdleWidgetOrder(ids, pinned) } },
            onDismiss = { showSelector = false }
        )
    }
}

@Composable
private fun IdleMasterCard(
    enabled: Boolean,
    offAfterTap: Boolean,
    offAfterLongPress: Boolean,
    offWhenStored: Boolean,
    landscape: Boolean,
    modeRepository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
    SectionCard {
        Column(modifier = Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SwitchRow(
                title = stringResource(R.string.idle_widgets_enable),
                subtitle = stringResource(R.string.idle_widgets_desc),
                checked = enabled,
                modifier = Modifier.searchAnchor("idle.enable"),
                onCheckedChange = {
                    scope.launch {
                        if (landscape) modeRepository.setIdleWidgetsEnabledLandscape(it) else modeRepository.setIdleWidgetsEnabled(it)
                    }
                }
            )
            if (enabled) {
                SettingsDivider(0.3f)
                SwitchRow(
                    title = stringResource(R.string.idle_off_after_tap_title),
                    subtitle = stringResource(R.string.idle_off_after_tap_desc),
                    checked = offAfterTap,
                    modifier = Modifier.searchAnchor("idle.off_tap"),
                    onCheckedChange = { scope.launch { modeRepository.setIdleOffAfterTapClose(landscape, it) } }
                )
                SwitchRow(
                    title = stringResource(R.string.idle_off_after_long_press_title),
                    subtitle = stringResource(R.string.idle_off_after_long_press_desc),
                    checked = offAfterLongPress,
                    modifier = Modifier.searchAnchor("idle.off_long_press"),
                    onCheckedChange = { scope.launch { modeRepository.setIdleOffAfterLongPress(landscape, it) } }
                )
                SwitchRow(
                    title = stringResource(R.string.idle_off_when_stored_title),
                    subtitle = stringResource(R.string.idle_off_when_stored_desc),
                    checked = offWhenStored,
                    modifier = Modifier.searchAnchor("idle.off_stored"),
                    onCheckedChange = { scope.launch { modeRepository.setIdleOffWhenStored(landscape, it) } }
                )
            }
            Text(
                text = stringResource(
                    if (landscape) R.string.idle_orientation_note_landscape else R.string.idle_orientation_note_portrait
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IdleSelectCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    context: Context,
    selected: List<IdleWidgetKind>,
    dndAccess: Boolean,
    onOpenWeatherSettings: () -> Unit,
    onOpenNetSpeedSettings: () -> Unit,
    onOpenCalendarSettings: () -> Unit,
    onShowSelector: () -> Unit
) {
    SectionCard(modifier = Modifier.searchAnchor("idle.select")) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            IdleSelectHeader(selected = selected)
            selected.forEach { kind ->
                SelectedWidgetRow(
                    kind = kind,
                    settings = settings,
                    onOpenWeatherSettings = onOpenWeatherSettings,
                    onOpenNetSpeedSettings = onOpenNetSpeedSettings,
                    onOpenCalendarSettings = onOpenCalendarSettings
                )
            }
            FilledTonalButton(
                onClick = onShowSelector,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.idle_select_button), fontWeight = FontWeight.SemiBold)
            }
            IdleSelectOptions(
                settings = settings,
                repository = repository,
                scope = scope,
                context = context,
                selected = selected,
                dndAccess = dndAccess
            )
        }
    }
}

@Composable
private fun IdleSelectHeader(selected: List<IdleWidgetKind>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.idle_select_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            SettingsDescription(stringResource(R.string.idle_select_desc))
        }
        CountBadge("${selected.size} / ${IdleWidgetKind.values().size}")
    }
}

@Composable
private fun SelectedWidgetRow(
    kind: IdleWidgetKind,
    settings: SmartIslandSettings,
    onOpenWeatherSettings: () -> Unit,
    onOpenNetSpeedSettings: () -> Unit,
    onOpenCalendarSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KindIcon(kind, checked = true)
        Text(
            text = stringResource(kindTitle(kind)),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        val openSettings: (() -> Unit)? = when (kind) {
            IdleWidgetKind.Weather, IdleWidgetKind.Wind -> onOpenWeatherSettings
            IdleWidgetKind.NetSpeed -> onOpenNetSpeedSettings
            IdleWidgetKind.Date -> onOpenCalendarSettings
            else -> null
        }
        if (openSettings != null) {
            IconButton(onClick = openSettings, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(
                        when (kind) {
                            IdleWidgetKind.NetSpeed -> R.string.net_cfg_open
                            IdleWidgetKind.Date -> R.string.cal_cfg_open
                            else -> R.string.weather_cfg_open
                        }
                    ),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (kind.id == settings.idleWidgetPinnedFirst) {
            Icon(
                imageVector = Icons.Rounded.PushPin,
                contentDescription = stringResource(R.string.idle_select_pinned),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun IdleSelectOptions(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    context: Context,
    selected: List<IdleWidgetKind>,
    dndAccess: Boolean
) {
    if (IdleWidgetKind.Battery in selected) {
        SwitchRow(
            title = stringResource(R.string.idle_battery_only_charging_title),
            subtitle = stringResource(R.string.idle_battery_only_charging_desc),
            checked = settings.batteryOnlyWhenCharging,
            modifier = Modifier.searchAnchor("idle.battery_only_charging"),
            onCheckedChange = { scope.launch { repository.setBatteryOnlyWhenCharging(it) } }
        )
    }
    if (IdleWidgetKind.NextAlarm in selected) {
        SwitchRow(
            title = stringResource(R.string.idle_alarm_hide_far_title),
            subtitle = stringResource(R.string.idle_alarm_hide_far_desc),
            checked = settings.idleAlarmHideBeyond12h,
            onCheckedChange = { scope.launch { repository.setIdleAlarmHideBeyond12h(it) } }
        )
    }
    if (IdleWidgetKind.Modes in selected && !dndAccess) {
        SettingsDescription(stringResource(R.string.idle_modes_access_desc))
        FilledTonalButton(
            onClick = {
                runCatchingLogged("IdleWidgetsSettingsSection", "Could not open the notification policy settings") {
                    context.startActivity(
                        Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.idle_modes_access_button), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun IdleTimersCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = stringResource(R.string.idle_timers_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TimerSlider(
                title = stringResource(R.string.idle_tick_timer_title),
                hint = stringResource(R.string.idle_tick_timer_desc),
                stored = settings.idleGlyphTickSeconds,
                range = IdleSlots.MIN_TICK_SECONDS..IdleSlots.MAX_TICK_SECONDS,
                steps = Math.round((IdleSlots.MAX_TICK_SECONDS - IdleSlots.MIN_TICK_SECONDS) * 2f) - 1,
                modifier = Modifier.searchAnchor("idle.tick_timer"),
                label = { stringResource(R.string.idle_seconds_decimal, it) },
                onCommit = { scope.launch { repository.setIdleGlyphTickSeconds(it) } }
            )
            SwitchRow(
                title = stringResource(R.string.idle_beat_only_change_title),
                subtitle = stringResource(R.string.idle_beat_only_change_desc),
                checked = settings.idleBeatOnlyOnChange,
                modifier = Modifier.searchAnchor("idle.beat_only_change"),
                onCheckedChange = { scope.launch { repository.setIdleBeatOnlyOnChange(it) } }
            )
            TimerSlider(
                title = stringResource(R.string.idle_switch_timer_title),
                hint = stringResource(R.string.idle_switch_timer_desc),
                stored = settings.idleWidgetCycleSeconds.toFloat(),
                range = IdleWidgets.MIN_CYCLE_SECONDS.toFloat()..IdleWidgets.MAX_CYCLE_SECONDS.toFloat(),
                steps = IdleWidgets.MAX_CYCLE_SECONDS - IdleWidgets.MIN_CYCLE_SECONDS - 1,
                modifier = Modifier.searchAnchor("idle.switch_timer"),
                label = { stringResource(R.string.idle_widgets_seconds, Math.round(it)) },
                onCommit = { scope.launch { repository.setIdleWidgetCycleSeconds(IdleWidgetOff.normalizeCycleSeconds(Math.round(it))) } }
            )
            SettingsDescription(stringResource(R.string.idle_timers_shared_note))
        }
    }
}

@Composable
private fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    SettingsCard(modifier = modifier) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            SettingsDescription(subtitle)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun TimerSlider(
    title: String,
    hint: String,
    stored: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
    label: @Composable (Float) -> String,
    onCommit: (Float) -> Unit
) {
    var value by remember(stored) { mutableFloatStateOf(stored) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label(value),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { onCommit(value) },
            valueRange = range,
            steps = steps
        )
        SettingsDescription(hint)
    }
}
