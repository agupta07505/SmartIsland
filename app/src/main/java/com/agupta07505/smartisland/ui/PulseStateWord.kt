/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.PulsePeak
import com.agupta07505.smartisland.model.StateWord
import com.agupta07505.smartisland.ui.IslandText as Text

@StringRes
internal fun StateWord.labelRes(): Int = when (this) {
    StateWord.Connected -> R.string.state_word_connected
    StateWord.Disconnected -> R.string.state_word_disconnected
    StateWord.PluggedIn -> R.string.state_word_plugged_in
    StateWord.Unplugged -> R.string.state_word_unplugged
    StateWord.On -> R.string.state_word_on
    StateWord.Off -> R.string.state_word_off
}

/** The localized word of [peak], empty for a peak that has none; measured and drawn from the same string. */
@Composable
internal fun statePulseText(peak: PulsePeak.Peak): String = peak.stateWord?.let { stringResource(it.labelRes()) }.orEmpty()

/**
 * Value of the wide pulse: the state word of a connection or device-state card, in the bold value style and the card's
 * accent colour; every other card keeps the settled pill's own value ([CollapsedValueContent])
 */
@Composable
internal fun PeakValue(
    card: IslandNotification,
    peak: PulsePeak.Peak,
    stateText: String,
    settings: SmartIslandSettings,
    modifier: Modifier = Modifier
) {
    if (peak.stateWord == null) {
        if (card.mode == IslandMode.Hotspot) {
            HotspotCollapsedRight(card, settings.forCard(card), label = true)
        } else {
            CollapsedValueContent(card.mode, card, settings)
        }
    } else {
        // the opposite event's word cross-fades and its accent eases, the pulse itself goes on
        val accent = morphColor(pulseAccent(card, settings))
        MorphSwap(stateText, direction = rememberMorphDirection(card.category)) { shown ->
            Text(
                text = shown,
                modifier = modifier,
                color = accent,
                fontSize = PEAK_TEXT_SP.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
