/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Rect
import android.view.accessibility.AccessibilityWindowInfo
import com.agupta07505.smartisland.ui.IslandViewModel
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.ShadeDetector
import com.agupta07505.smartisland.util.ShadeTitles
import com.agupta07505.smartisland.util.ShadeWindow
import com.agupta07505.smartisland.util.runCatchingLogged
import java.util.Locale

/**
 * From the accessibility window list
 * The service keeps the window-to-package lookup (own cache), so it comes in as [windowPackage]
 */
internal class OverlayShadeTracker(
    private val service: AccessibilityService,
    private val viewModel: () -> IslandViewModel,
    private val windowPackage: (AccessibilityWindowInfo) -> String?,
) {
    internal var shadeOpen = false
    private var shadeTitlesLocale: String? = null
    private var shadeTitlesCache: ShadeTitles = ShadeTitles.ENGLISH

    internal fun refreshShadeState(
        locked: Boolean,
        windowList: List<AccessibilityWindowInfo>
    ) {
        val windowsNow = runCatchingLogged(TAG, "Reading windows for the shade failed") {
            windowList.mapNotNull { w ->
                if (w.type != AccessibilityWindowInfo.TYPE_SYSTEM) return@mapNotNull null
                val pkg = windowPackage(w)
                if (pkg != ShadeDetector.SYSTEM_UI) return@mapNotNull null
                val bounds = Rect()
                w.getBoundsInScreen(bounds)
                ShadeWindow(
                    packageName = pkg,
                    title = w.title?.toString(),
                    isSystemType = true,
                    isActive = w.isActive,
                    isFocused = w.isFocused,
                    top = bounds.top,
                    bottom = bounds.bottom
                )
            }
        } ?: return
        val open = ShadeDetector.isShadeOpen(
            windows = windowsNow,
            displayHeight = service.resources.displayMetrics.heightPixels,
            keyguardLocked = locked,
            titles = shadeTitles()
        )
        if (open == shadeOpen) return
        shadeOpen = open
        AppLogRecorder.i(
            "Shade",
            "open=$open locked=$locked systemUiWindows=" + windowsNow.joinToString {
                "[title=${it.title} active=${it.isActive} focused=${it.isFocused} top=${it.top} bottom=${it.bottom}]"
            }
        )
        viewModel().setShadeOpen(open)
    }

    private fun shadeTitles(): ShadeTitles {
        val locale = Locale.getDefault().toLanguageTag()
        if (locale == shadeTitlesLocale) return shadeTitlesCache
        val systemUi = ShadeDetector.SYSTEM_UI
        val res = try {
            service.packageManager.getResourcesForApplication(systemUi)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        fun read(name: String): String? {
            if (res == null) return null
            val id = res.getIdentifier(name, "string", systemUi)
            if (id == 0) return null
            val text = try {
                res.getString(id)
            } catch (_: Resources.NotFoundException) {
                return null
            }
            return text.trim { it == '.' || it.isWhitespace() }.takeIf { it.isNotBlank() }
        }
        val english = ShadeTitles.ENGLISH
        val titles = ShadeTitles(
            shade = listOfNotNull(
                read("accessibility_desc_notification_shade"),
                read("accessibility_desc_quick_settings")
            ) + english.shade,
            lockScreen = listOfNotNull(read("accessibility_desc_lock_screen")) + english.lockScreen
        )
        shadeTitlesLocale = locale
        shadeTitlesCache = titles
        return titles
    }

    private companion object {
        const val TAG = "SmartIslandOverlayService"
    }
}
