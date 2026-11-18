/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.LedTick
import com.agupta07505.smartisland.model.LedTickConfig
import com.agupta07505.smartisland.ui.LedTickPreview
import com.agupta07505.smartisland.ui.SettingsCard
import com.agupta07505.smartisland.ui.glass.Switch
import com.agupta07505.smartisland.ui.search.searchAnchor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Smooth tick (the original) or LED footsteps tick. */
@Composable
internal fun LedTickSettingsCard(settings: SmartIslandSettings, repository: SmartIslandSettingsRepository) {
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val ledOn = settings.idleTickStyle == LedTick.STYLE_LED

    val config = remember(settings.ledTickPattern, settings.ledTickPaceMs, settings.ledTickFont, settings.ledTickColor,
        settings.ledTickStagger) {
        LedTickConfig(
            pattern = LedTick.parsePattern(settings.ledTickPattern),
            paceMs = LedTick.normalizePace(settings.ledTickPaceMs),
            fontId = settings.ledTickFont,
            colorId = settings.ledTickColor,
            stagger = settings.ledTickStagger
        )
    }

    SettingsCard(modifier = Modifier.searchAnchor("idle.led_tick")) {
        Box(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = stringResource(R.string.led_tick_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LedTickSwitchRow(ledOn = ledOn, scope = scope, repository = repository)
                if (ledOn) {
                    SettingsDivider(0.3f)

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        LedTickPreview(config = config)
                    }
                    LedTickPatternField(settings = settings, repository = repository, scope = scope, focus = focus)
                    LedTickPaceRow(settings = settings, repository = repository, scope = scope)
                    LedTickStaggerRow(settings = settings, repository = repository, scope = scope)
                    LedTickColorRow(settings = settings, repository = repository, scope = scope)

                    // Empty = built-in dot font, any other font (added ones included) is turned into dots
                    IslandFontPicker(
                        selected = settings.ledTickFont,
                        onSelected = { scope.launch { repository.setLedTickFont(it) } },
                        title = stringResource(R.string.led_tick_font_title),
                        description = stringResource(R.string.led_tick_font_desc),
                        inheritRow = stringResource(R.string.led_tick_font_builtin) to stringResource(R.string.led_tick_font_builtin_note)
                    )

                    Text(
                        text = stringResource(R.string.led_tick_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LedTickSwitchRow(
    ledOn: Boolean,
    scope: CoroutineScope,
    repository: SmartIslandSettingsRepository
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.led_tick_switch_title),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.led_tick_switch_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = ledOn,
            onCheckedChange = { on ->
                scope.launch { repository.setIdleTickStyle(if (on) LedTick.STYLE_LED else LedTick.STYLE_SMOOTH) }
            }
        )
    }
}

@Composable
private fun LedTickPatternField(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    focus: FocusManager
) {
    // Typed like the demo, written back in its clean form on commit
    var text by remember(settings.ledTickPattern) { mutableStateOf(settings.ledTickPattern) }
    val commit = {
        scope.launch { repository.setLedTickPattern(text) }
        Unit
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it.take(40) },
        singleLine = true,
        label = { Text(stringResource(R.string.led_tick_pattern_title)) },
        supportingText = { Text(stringResource(R.string.led_tick_pattern_desc)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            commit()
            focus.clearFocus()
        }),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { state -> if (!state.isFocused && text != settings.ledTickPattern) commit() }
    )
}

@Composable
private fun LedTickPaceRow(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
    // Own copy follows the finger, written once when it lifts
    var pace by remember(settings.ledTickPaceMs) { mutableFloatStateOf(settings.ledTickPaceMs.toFloat()) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.led_tick_pace_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.led_tick_ms, LedTick.normalizePace(Math.round(pace))),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = pace,
            onValueChange = { pace = it },
            onValueChangeFinished = { scope.launch { repository.setLedTickPaceMs(Math.round(pace)) } },
            valueRange = LedTick.MIN_PACE_MS.toFloat()..LedTick.MAX_PACE_MS.toFloat(),
            steps = (LedTick.MAX_PACE_MS - LedTick.MIN_PACE_MS) / LedTick.PACE_STEP_MS - 1
        )
        Text(
            text = stringResource(R.string.led_tick_pace_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LedTickStaggerRow(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
    // footsteps: the value steps half a pace after the glyph
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.led_tick_stagger_title),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.led_tick_stagger_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = settings.ledTickStagger,
            onCheckedChange = { on -> scope.launch { repository.setLedTickStagger(on) } }
        )
    }
}

@Composable
private fun LedTickColorRow(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.led_tick_color_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                LedTick.COLOR_RED to R.string.led_tick_color_red,
                LedTick.COLOR_AMBER to R.string.led_tick_color_amber,
                LedTick.COLOR_GREEN to R.string.led_tick_color_green,
                LedTick.COLOR_WHITE to R.string.led_tick_color_white
            ).forEach { (id, label) ->
                val modifier = Modifier.weight(1f)
                val padding = PaddingValues(horizontal = 4.dp)
                if (settings.ledTickColor == id) {
                    Button(onClick = { }, modifier = modifier,
                        contentPadding = padding) { Text(stringResource(label), maxLines = 1) }
                } else {
                    OutlinedButton(
                        onClick = { scope.launch { repository.setLedTickColor(id) } },
                        modifier = modifier,
                        contentPadding = padding
                    ) { Text(stringResource(label), maxLines = 1) }
                }
            }
        }
    }
}
