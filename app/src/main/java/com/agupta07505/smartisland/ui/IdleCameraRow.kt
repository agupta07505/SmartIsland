/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.agupta07505.smartisland.model.CameraBand
import com.agupta07505.smartisland.model.SlotSide
import com.agupta07505.smartisland.model.idleStripsFor
import com.agupta07505.smartisland.model.weights

/** Row of the idle moment pills (day moment, hour tick, weather tick). */
@Composable
internal fun IdleCameraRow(
    pillWidthDp: Float,
    band: CameraBand,
    side: SlotSide,
    modifier: Modifier,
    first: @Composable (Modifier) -> Unit,
    second: (@Composable (Modifier) -> Unit)? = null
) {
    val (left, mid, right) = idleStripsFor(pillWidthDp, band).weights()
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        when (side) {
            SlotSide.Both -> {
                first(Modifier.weight(left).fillMaxHeight())
                Spacer(Modifier.weight(mid))
                if (second != null) second(Modifier.weight(right).fillMaxHeight()) else Spacer(Modifier.weight(right))
            }
            SlotSide.RightOfCamera -> {
                Spacer(Modifier.weight(left + mid))
                Row(Modifier.weight(right).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    first(Modifier.weight(1f).fillMaxHeight())
                    second?.invoke(Modifier.weight(1f).fillMaxHeight())
                }
            }
            SlotSide.LeftOfCamera -> {
                Row(Modifier.weight(left).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    first(Modifier.weight(1f).fillMaxHeight())
                    second?.invoke(Modifier.weight(1f).fillMaxHeight())
                }
                Spacer(Modifier.weight(mid + right))
            }
        }
    }
}
