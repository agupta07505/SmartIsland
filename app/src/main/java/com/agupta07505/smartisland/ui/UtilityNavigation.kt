/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.Context
import android.content.Intent
import com.agupta07505.smartisland.MainActivity
import com.agupta07505.smartisland.ui.pickers.PickerActivity
import com.agupta07505.smartisland.ui.pickers.PickerKinds
import com.agupta07505.smartisland.util.runCatchingLogged

/** Opens the app's own screens from the utility cards, the part of the utility actions that needs the UI layer. */
object UtilityNavigation {
    const val EXTRA_OPEN_SECTION = "open_section"
    const val SECTION_APP_SHORTCUTS = "app_shortcuts"

    fun openShortcutSettings(context: Context) {
        val intent = Intent(context.applicationContext, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_SECTION, SECTION_APP_SHORTCUTS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        runCatchingLogged("UtilityActions", "Could not open the shortcut settings") { context.startActivity(intent) }
    }

    fun openPicker(context: Context, picker: String) {
        val kind = PickerKinds.forRequest(picker) ?: return
        runCatching { context.startActivity(PickerActivity.intentFor(context, kind)) }
            .onFailure { openShortcutSettings(context) }
    }
}
