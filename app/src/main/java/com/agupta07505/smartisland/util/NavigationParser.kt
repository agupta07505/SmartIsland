/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.service.notification.StatusBarNotification
import java.util.regex.Pattern

enum class TurnDirection {
    LEFT,
    RIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    U_TURN,
    STRAIGHT,
    ROUNDABOUT,
    DESTINATION
}

data class NavigationInfo(
    val packageName: String,
    val distanceToTurnText: String,
    val maneuverTitle: String,
    val subText: String? = null,
    val turnDirection: TurnDirection = TurnDirection.STRAIGHT
)

object NavigationParser {

    private val NAVIGATION_PACKAGES = setOf(
        "com.google.android.apps.maps",
        "com.waze",
        "com.maptls.app",
        "com.mapmyindia.maps",
        "com.sygic.aura",
        "com.huawei.maps.app",
        "com.autonavi.minimap",
        "com.baidu.BaiduMap",
        "com.here.app.maps",
        "com.tomtom.gplay.navapp",
        "net.osmand",
        "ru.yandex.yandexnavi",
        "ru.yandex.yandexmaps",
        "com.locnall.KimGiSa",
        "com.nhn.android.nmap"
    )

    private val DISTANCE_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*(?:m|km|ft|mi|miles?|meters?)\\b", Pattern.CASE_INSENSITIVE)
    private val GENERIC_DISTANCE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:m|km|ft|mi|miles?|meters?)\\b", Pattern.CASE_INSENSITIVE)

    private val NAVIGATION_MANEUVER_KEYWORDS = listOf(
        "turn", "head", "continue", "exit", "merge", "destination", "arrived", "arriving",
        "onto", "towards", "via", "route", "ramp", "fork", "keep left", "keep right", "u-turn", "roundabout"
    )

    fun isNavigationApp(packageName: String): Boolean {
        return packageName in NAVIGATION_PACKAGES
    }

    fun parse(sbn: StatusBarNotification): NavigationInfo? {
        val packageName = sbn.packageName
        val notification = sbn.notification ?: return null
        val isCategoryNav = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            notification.category == Notification.CATEGORY_NAVIGATION
        } else {
            notification.category == "navigation"
        }

        if (!isCategoryNav && !isNavigationApp(packageName)) {
            return null
        }

        val extras = notification.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        if (title.isBlank() && text.isBlank()) return null

        val combinedContent = "$title $text".lowercase()

        // Exclude OEM screen recording, voice recording, calls, and hotspot notifications from being hijacked by Navigation
        if (OemDeviceRules.isNonNavigationContent(packageName, combinedContent)) {
            return null
        }

        // 1. Extract Distance to Turn
        val leadingDistMatcher = DISTANCE_PATTERN.matcher(title.trim())
        val genericDistMatcher = GENERIC_DISTANCE_PATTERN.matcher("$title $text")

        val hasLeadingDist = leadingDistMatcher.find()
        val hasGenericDist = if (!hasLeadingDist) genericDistMatcher.find() else false
        val hasDistPattern = hasLeadingDist || hasGenericDist

        val hasNavKeywords = NAVIGATION_MANEUVER_KEYWORDS.any { combinedContent.contains(it) }

        // If not a recognized navigation app, require explicit distance or navigation maneuver keywords
        if (!isNavigationApp(packageName) && !hasDistPattern && !hasNavKeywords) {
            return null
        }

        val distanceText = when {
            hasLeadingDist -> leadingDistMatcher.group(0)
            hasGenericDist -> genericDistMatcher.group(0)
            isNavigationApp(packageName) -> "In 200 m"
            else -> return null
        }

        // 2. Determine Turn Direction
        val turnDirection = parseTurnDirection(combinedContent)

        // 3. Clean up Maneuver Title
        val maneuverTitle = when {
            title.isNotBlank() && !title.startsWith(distanceText, ignoreCase = true) -> title
            text.isNotBlank() -> text
            else -> "Head straight"
        }

        val formattedSubText = when {
            subText != null && subText.isNotBlank() -> subText
            text.isNotBlank() && text != maneuverTitle -> text
            else -> "Turn-by-turn navigation"
        }

        return NavigationInfo(
            packageName = packageName,
            distanceToTurnText = distanceText,
            maneuverTitle = maneuverTitle,
            subText = formattedSubText,
            turnDirection = turnDirection
        )
    }

    fun parseTurnDirection(content: String): TurnDirection {
        val lower = content.lowercase()
        return when {
            lower.contains("u-turn") || lower.contains("uturn") || lower.contains("make a u turn") || lower.contains("make a u-turn") -> TurnDirection.U_TURN
            lower.contains("slight left") || lower.contains("bear left") || lower.contains("keep left") || lower.contains("veer left") -> TurnDirection.SLIGHT_LEFT
            lower.contains("slight right") || lower.contains("bear right") || lower.contains("keep right") || lower.contains("veer right") -> TurnDirection.SLIGHT_RIGHT
            lower.contains("turn left") || lower.contains("left onto") || lower.contains("left on") || lower.contains("take the left") || lower.contains("sharp left") -> TurnDirection.LEFT
            lower.contains("turn right") || lower.contains("right onto") || lower.contains("right on") || lower.contains("take the right") || lower.contains("sharp right") -> TurnDirection.RIGHT
            lower.contains("roundabout") || lower.contains("rotary") || lower.contains("traffic circle") -> TurnDirection.ROUNDABOUT
            lower.contains("arrived") || lower.contains("destination") || lower.contains("reached") || lower.contains("you have arrived") -> TurnDirection.DESTINATION
            // Word-boundary direction matching only when not preceded by numbers/time words
            Regex("\\b(turn|go|head|take)\\s+left\\b").containsMatchIn(lower) -> TurnDirection.LEFT
            Regex("\\b(turn|go|head|take)\\s+right\\b").containsMatchIn(lower) -> TurnDirection.RIGHT
            !lower.contains("left to") && !lower.contains("min left") && !lower.contains("mins left") && !lower.contains("mb left") && !lower.contains("gb left") && !lower.contains("sec left") && Regex("\\bleft\\b").containsMatchIn(lower) && (lower.contains("onto") || lower.contains("street") || lower.contains("road") || lower.contains("ave")) -> TurnDirection.LEFT
            !lower.contains("right now") && !lower.contains("all right") && Regex("\\bright\\b").containsMatchIn(lower) && (lower.contains("onto") || lower.contains("street") || lower.contains("road") || lower.contains("ave")) -> TurnDirection.RIGHT
            else -> TurnDirection.STRAIGHT
        }
    }
}
