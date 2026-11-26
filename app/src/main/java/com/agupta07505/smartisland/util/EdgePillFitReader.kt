/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.content.res.Configuration
import com.agupta07505.smartisland.data.CutoutPlacement
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CutoutArea
import com.agupta07505.smartisland.model.EdgePillFit
import com.agupta07505.smartisland.model.EdgePillTuning

object EdgePillFitReader {
    fun fitted(context: Context, tuning: EdgePillTuning, settings: SmartIslandSettings): EdgePillTuning? {
        if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val state = LandscapeCameraDetector.read(context, true, settings)
            val camera = state.camera ?: return null
            if (camera.widthDp <= 0f) return null
            // the camera is assumed unless the system reported it: placed by hand, or the guess when nothing is reported
            val portraitWidthDp = minOf(state.screenWidthDp, state.screenHeightDp)
            val assumed = CutoutPlacement.placedFor(settings, portraitWidthDp) != null ||
                !CameraCutoutDetector.detect(context).hasHardwareCutout
            return EdgePillFit.fitLandscape(tuning, camera.xOffsetDp, camera.widthDp, state.screenWidthDp,
                camera.lengthDp, assumed)
        }
        CameraCutoutDetector.invalidate()
        val screenWidthDp = context.resources.configuration.screenWidthDp.toFloat()
        val placed = CutoutPlacement.placedFor(settings, screenWidthDp)?.clampedTo(screenWidthDp)
        val area: CutoutArea? = placed ?: CameraCutoutDetector.detectedArea(CameraCutoutDetector.detect(context))
        return area?.let { EdgePillFit.fitPortrait(tuning, it, assumed = placed != null) }
    }
}
