/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.animation.animateContentSize
import com.agupta07505.smartisland.model.IdleSchedule
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.key
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import com.agupta07505.smartisland.ui.glass.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import com.agupta07505.smartisland.ui.glass.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.agupta07505.smartisland.util.DndModeReader
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IdleSlots
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.IdleWidgetOff
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.ui.search.searchAnchor
import kotlinx.coroutines.launch

/**
 * Settings > Idle widgets, three cards: the switches (one set for portrait and one for landscape, chosen by the
 * tabs), the widget selector (every widget can be ticked), and the two timers shared by both orientations (the
 * glyphs' sideways tick and the automatic widget switch).
 */
@Composable
fun IdleWidgetsSettingsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    onOpenWeatherSettings: () -> Unit = {},
    onOpenNetSpeedSettings: () -> Unit = {},
    onOpenCalendarSettings: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(OrientationTab.Portrait) }
    val landscape = tab == OrientationTab.Landscape
    var showSelector by rememberSaveable { mutableStateOf(false) }

    val enabled = if (landscape) settings.idleWidgetsEnabledLandscape else settings.idleWidgetsEnabled
    val offAfterTap = if (landscape) settings.idleOffAfterTapCloseLandscape else settings.idleOffAfterTapClose
    val offAfterLongPress = if (landscape) settings.idleOffAfterLongPressLandscape else settings.idleOffAfterLongPress
    val offWhenStored = if (landscape) settings.idleOffWhenStoredLandscape else settings.idleOffWhenStored
    val selected = remember(settings.idleWidgetIds, settings.idleWidgetPinnedFirst) {
        IdleSchedule.ordered(IdleWidgets.parse(settings.idleWidgetIds), settings.idleWidgetPinnedFirst)
    }

    // The mode widget can only tell Bedtime / Driving / ... apart with Do Not Disturb access; the answer is read
    // again when the user comes back from the system page.
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
        OrientationTabs(selected = tab, onSelected = { tab = it })

        SectionCard {
            Column(modifier = Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SwitchRow(
                    title = stringResource(R.string.idle_widgets_enable),
                    subtitle = stringResource(R.string.idle_widgets_desc),
                    checked = enabled,
                    modifier = Modifier.searchAnchor("idle.enable"),
                    onCheckedChange = {
                        scope.launch {
                            if (landscape) repository.setIdleWidgetsEnabledLandscape(it) else repository.setIdleWidgetsEnabled(it)
                        }
                    }
                )
                if (enabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SwitchRow(
                        title = stringResource(R.string.idle_off_after_tap_title),
                        subtitle = stringResource(R.string.idle_off_after_tap_desc),
                        checked = offAfterTap,
                        modifier = Modifier.searchAnchor("idle.off_tap"),
                        onCheckedChange = { scope.launch { repository.setIdleOffAfterTapClose(landscape, it) } }
                    )
                    SwitchRow(
                        title = stringResource(R.string.idle_off_after_long_press_title),
                        subtitle = stringResource(R.string.idle_off_after_long_press_desc),
                        checked = offAfterLongPress,
                        modifier = Modifier.searchAnchor("idle.off_long_press"),
                        onCheckedChange = { scope.launch { repository.setIdleOffAfterLongPress(landscape, it) } }
                    )
                    SwitchRow(
                        title = stringResource(R.string.idle_off_when_stored_title),
                        subtitle = stringResource(R.string.idle_off_when_stored_desc),
                        checked = offWhenStored,
                        modifier = Modifier.searchAnchor("idle.off_stored"),
                        onCheckedChange = { scope.launch { repository.setIdleOffWhenStored(landscape, it) } }
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

        SectionCard(modifier = Modifier.searchAnchor("idle.select")) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        Text(
                            text = stringResource(R.string.idle_select_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    CountBadge("${selected.size} / ${IdleWidgetKind.values().size}")
                }
                selected.forEach { kind ->
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
                        // A gear on the right of the widgets that have settings of their own (only the ticked ones are listed here).
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
                FilledTonalButton(
                    onClick = { showSelector = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.idle_select_button), fontWeight = FontWeight.SemiBold)
                }
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
                    Text(
                        text = stringResource(R.string.idle_modes_access_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
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
        }

        IdleScheduleCard(settings = settings, repository = repository, selected = selected, modifier = Modifier.searchAnchor("idle.schedule"))

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
                Text(
                    text = stringResource(R.string.idle_timers_shared_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IdleUnitsCard(settings = settings, repository = repository)

        // Weather, Calendar and Network speed settings live on their own pages, opened from the gear on the widget's row in the list above.

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
private fun WidgetSelectorDialog(
    storedIds: String,
    pinnedFirst: String,
    onChange: (String) -> Unit,
    /** Saves the order together with the pinned widget (empty = none). */
    onOrder: (ids: String, pinned: String) -> Unit,
    onDismiss: () -> Unit,
    onOpenWeather: () -> Unit = {},
    onOpenNetSpeed: () -> Unit = {}
) {
    val selected = remember(storedIds) { IdleWidgets.parse(storedIds) }
    val pinned = remember(selected, pinnedFirst) { IdleWidgetKind.fromId(pinnedFirst)?.takeIf { it in selected } }
    val others = IdleWidgetKind.values().filter { it !in selected }
    // The dragged row follows the finger on the rows as they rest; the order is saved once, when the finger lifts.
    var dragged by remember { mutableStateOf<IdleWidgetKind?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragTarget by remember { mutableStateOf<IdleWidgetKind?>(null) }
    val bounds = remember { HashMap<String, androidx.compose.ui.geometry.Rect>() }

    // The row the dragged one would take the place of: the farthest one whose middle the dragged row's middle has
    // crossed. The pinned row stays first.
    fun dropFor(kind: IdleWidgetKind, offsetY: Float): IdleWidgetKind? {
        val from = bounds[kind.id] ?: return null
        val centerY = from.center.y + offsetY
        var best: IdleWidgetKind? = null
        var bestDistance = 0f
        for (other in selected) {
            if (other == kind || other == pinned) continue
            val rect = bounds[other.id] ?: continue
            val crossed = if (rect.center.y > from.center.y) centerY >= rect.center.y else centerY <= rect.center.y
            val distance = kotlin.math.abs(rect.center.y - from.center.y)
            if (crossed && distance > bestDistance) {
                best = other
                bestDistance = distance
            }
        }
        return best
    }

    fun toggle(kind: IdleWidgetKind) {
        val next = IdleWidgets.toggle(storedIds, kind)
        if (kind == pinned && next != storedIds) onOrder(next, "") else onChange(next)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 12.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        icon = {
            Icon(
                imageVector = Icons.Rounded.Widgets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.idle_select_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                CountBadge("${selected.size} / ${IdleWidgetKind.values().size}")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onChange(IdleWidgetOff.allIds()) }) {
                        Text(stringResource(R.string.idle_select_all), fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { onOrder(IdleWidgets.DEFAULT_IDS, "") }) {
                        Text(stringResource(R.string.idle_select_reset), fontWeight = FontWeight.SemiBold)
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    selected.forEach { kind ->
                        key(kind.id) {
                            val isDragged = dragged == kind
                            val isPinned = pinned == kind
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { bounds[kind.id] = it.boundsInParent() }
                                    .zIndex(if (isDragged) 1f else 0f)
                                    .graphicsLayer { translationY = if (isDragged) dragOffsetY else 0f }
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isDragged -> MaterialTheme.colorScheme.surfaceVariant
                                            dragTarget == kind -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else -> androidx.compose.ui.graphics.Color.Transparent
                                        }
                                    )
                                    .clickable(enabled = dragged == null) { toggle(kind) }
                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Handle: drag to move the row. The pinned widget leads the rotation, so it has none.
                                if (isPinned || selected.size < 2) {
                                    Spacer(Modifier.width(24.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.DragHandle,
                                        contentDescription = stringResource(R.string.idle_select_drag, stringResource(kindTitle(kind))),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .pointerInput(kind, storedIds, pinned) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        dragged = kind
                                                        dragOffsetY = 0f
                                                        dragTarget = null
                                                    },
                                                    onDrag = { change, amount ->
                                                        change.consume()
                                                        dragOffsetY += amount.y
                                                        dragTarget = dropFor(kind, dragOffsetY)
                                                    },
                                                    onDragEnd = {
                                                        val target = dropFor(kind, dragOffsetY)
                                                        dragged = null
                                                        dragOffsetY = 0f
                                                        dragTarget = null
                                                        if (target != null) {
                                                            val moved = IdleSchedule.move(storedIds, kind, target)
                                                            if (moved != storedIds) onOrder(moved, pinned?.id ?: "")
                                                        }
                                                    },
                                                    onDragCancel = {
                                                        dragged = null
                                                        dragOffsetY = 0f
                                                        dragTarget = null
                                                    }
                                                )
                                            }
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                KindIcon(kind, checked = true)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(kindTitle(kind)),
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2
                                )
                                // Weather's and Network speed's settings tabs: a gear on the right of the row, only while the widget is on.
                                if (kind == IdleWidgetKind.Weather) {
                                    IconButton(onClick = onOpenWeather, modifier = Modifier.size(36.dp)) {
                                        Icon(
                                            imageVector = Icons.Rounded.Settings,
                                            contentDescription = stringResource(R.string.weather_cfg_open),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (kind == IdleWidgetKind.NetSpeed) {
                                    IconButton(onClick = onOpenNetSpeed, modifier = Modifier.size(36.dp)) {
                                        Icon(
                                            imageVector = Icons.Rounded.Settings,
                                            contentDescription = stringResource(R.string.net_cfg_open),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                // Pin: this widget always comes first. Tap it again to let it go.
                                IconButton(
                                    onClick = {
                                        val next = IdleSchedule.togglePinned(pinned?.id ?: "", kind)
                                        onOrder(if (next.isEmpty()) storedIds else IdleSchedule.moveToFront(storedIds, kind), next)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PushPin,
                                        contentDescription = stringResource(
                                            if (isPinned) R.string.idle_select_unpin else R.string.idle_select_pin,
                                            stringResource(kindTitle(kind))
                                        ),
                                        tint = if (isPinned) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                // The last widget stays ticked: the master switch is what turns the widgets off.
                                Checkbox(
                                    checked = true,
                                    enabled = selected.size > 1 && dragged == null,
                                    onCheckedChange = { toggle(kind) }
                                )
                            }
                        }
                    }
                    others.forEach { kind ->
                        key(kind.id) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { toggle(kind) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KindIcon(kind, checked = false)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(kindTitle(kind)),
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(kindDesc(kind)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Checkbox(checked = false, onCheckedChange = { toggle(kind) })
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.idle_select_order_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.idle_select_last_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.idle_select_done), fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
private fun KindIcon(kind: IdleWidgetKind, checked: Boolean) {
    val tint = kindTint(kind)
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (checked) tint.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = kindIcon(kind),
            contentDescription = null,
            tint = if (checked) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun CountBadge(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
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
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
