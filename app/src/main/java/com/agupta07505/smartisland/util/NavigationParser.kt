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
        "com.sygic.aura"
    )

    private val DISTANCE_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*(?:m|km|ft|mi|miles?|meters?)\\b", Pattern.CASE_INSENSITIVE)
    private val GENERIC_DISTANCE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:m|km|ft|mi|miles?|meters?)\\b", Pattern.CASE_INSENSITIVE)

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

        // 1. Extract Distance to Turn
        val leadingDistMatcher = DISTANCE_PATTERN.matcher(title.trim())
        val genericDistMatcher = GENERIC_DISTANCE_PATTERN.matcher("$title $text")

        val distanceText = when {
            leadingDistMatcher.find() -> leadingDistMatcher.group(0)
            genericDistMatcher.find() -> genericDistMatcher.group(0)
            else -> "In 200 m"
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
        return when {
            content.contains("u-turn") || content.contains("uturn") || content.contains("make a u turn") -> TurnDirection.U_TURN
            content.contains("slight left") || content.contains("bear left") || content.contains("keep left") -> TurnDirection.SLIGHT_LEFT
            content.contains("slight right") || content.contains("bear right") || content.contains("keep right") -> TurnDirection.SLIGHT_RIGHT
            content.contains("turn left") || content.contains("left onto") || content.contains("left on") -> TurnDirection.LEFT
            content.contains("turn right") || content.contains("right onto") || content.contains("right on") -> TurnDirection.RIGHT
            content.contains("roundabout") || content.contains("rotary") || content.contains("traffic circle") -> TurnDirection.ROUNDABOUT
            content.contains("arrived") || content.contains("destination") || content.contains("reached") -> TurnDirection.DESTINATION
            content.contains("left") -> TurnDirection.LEFT
            content.contains("right") -> TurnDirection.RIGHT
            else -> TurnDirection.STRAIGHT
        }
    }
}
