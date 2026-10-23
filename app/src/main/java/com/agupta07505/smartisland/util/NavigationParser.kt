/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.service.notification.StatusBarNotification

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

/** What the expanded navigation card shows, split into its rows. */
data class NavigationCardText(
    /** "In 300 m" for the badge. */
    val distance: String,
    /** The maneuver itself ("Turn left onto MG Road"), without a leading distance. */
    val instruction: String,
    /** Extra instruction text such as a street or lane hint; empty when there is none. */
    val detail: String,
    /** Trip stats such as "24 min • 8.5 km" or "ETA 18:42"; empty when there are none. */
    val eta: String
)

object NavigationParser {

    fun isNavigationApp(packageName: String): Boolean = AppPackageCatalog.isNavigationApp(packageName)

    fun parse(sbn: StatusBarNotification): NavigationInfo? {
        val packageName = sbn.packageName

        // Delivery and ride apps track an order or a trip; they never give turn-by-turn guidance, even when one of
        // them tags its notification with the navigation category and a distance ("Your rider is 500 m away").
        if (AppPackageCatalog.isLiveActivityApp(packageName)) return null

        val notification = sbn.notification ?: return null
        val isCategoryNav = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            notification.category == Notification.CATEGORY_NAVIGATION
        } else {
            notification.category == "navigation"
        }
        val knownApp = isNavigationApp(packageName)

        if (!isCategoryNav && !knownApp) {
            return null
        }

        // A map app also posts ordinary notifications (Maps' "Your September Timeline", a saved-place tip, a traffic
        // alert).
        if (!isCategoryNav) {
            val running = (notification.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE)) != 0
            if (!running) return null
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

        val leadingDist = NavigationLocale.LEADING_DISTANCE.find(title.trim())
        val anyDist = if (leadingDist == null) NavigationLocale.ANY_DISTANCE.find("$title $text") else null
        val distanceMatch = leadingDist ?: anyDist
        val hasTurnDirection = parseTurnDirection(combinedContent) != TurnDirection.STRAIGHT
        val hasManeuverWording = hasTurnDirection || NavigationLocale.MANEUVER_WORDING.containsMatchIn(combinedContent)
        val hasTripStat = NavigationLocale.TRIP_STAT.containsMatchIn(combinedContent)

        if (knownApp) {
            // A known map app that categorises the notification as navigation is trusted.
            if (!isCategoryNav && !hasManeuverWording && distanceMatch == null && !hasTripStat) return null
        } else {
            // Any other app only gets here through the navigation category, which delivery, ride and tracking apps
            // use too.
            if (!hasManeuverWording) return null
            if (NavigationLocale.DELIVERY_OR_RIDE_WORDING.containsMatchIn(combinedContent)) return null
        }

        val distanceText = distanceMatch?.value ?: "In 200 m"

        val turnDirection = parseTurnDirection(combinedContent)

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

    private val ENGLISH_LEFT_WORD = Regex("\\b(turn|go|head|take)\\s+left\\b")
    private val ENGLISH_RIGHT_WORD = Regex("\\b(turn|go|head|take)\\s+right\\b")
    private val LEFT_WORD = Regex("\\bleft\\b")
    private val RIGHT_WORD = Regex("\\bright\\b")

    private val U_TURN = NavigationLocale.anyOf(NavigationLocale.U_TURN)
    private val SLIGHT_LEFT = NavigationLocale.anyOf(NavigationLocale.SLIGHT_LEFT)
    private val SLIGHT_RIGHT = NavigationLocale.anyOf(NavigationLocale.SLIGHT_RIGHT)
    private val LEFT = NavigationLocale.anyOf(NavigationLocale.LEFT)
    private val RIGHT = NavigationLocale.anyOf(NavigationLocale.RIGHT)
    private val ROUNDABOUT = NavigationLocale.anyOf(NavigationLocale.ROUNDABOUT)
    private val DESTINATION = NavigationLocale.anyOf(NavigationLocale.DESTINATION)

    fun parseTurnDirection(content: String): TurnDirection {
        val lower = content.lowercase()
        return when {
            U_TURN.containsMatchIn(lower) -> TurnDirection.U_TURN
            SLIGHT_LEFT.containsMatchIn(lower) -> TurnDirection.SLIGHT_LEFT
            SLIGHT_RIGHT.containsMatchIn(lower) -> TurnDirection.SLIGHT_RIGHT
            LEFT.containsMatchIn(lower) -> TurnDirection.LEFT
            RIGHT.containsMatchIn(lower) -> TurnDirection.RIGHT
            ROUNDABOUT.containsMatchIn(lower) -> TurnDirection.ROUNDABOUT
            DESTINATION.containsMatchIn(lower) -> TurnDirection.DESTINATION
            // Word-boundary direction matching only when not preceded by numbers/time words
            ENGLISH_LEFT_WORD.containsMatchIn(lower) -> TurnDirection.LEFT
            ENGLISH_RIGHT_WORD.containsMatchIn(lower) -> TurnDirection.RIGHT
            !lower.contains("left to") && !lower.contains("min left") && !lower.contains("mins left") && !lower.contains("mb left") && !lower.contains("gb left") && !lower.contains("sec left") && LEFT_WORD.containsMatchIn(lower) && (lower.contains("onto") || lower.contains("street") || lower.contains("road") || lower.contains("ave")) -> TurnDirection.LEFT
            !lower.contains("right now") && !lower.contains("all right") && RIGHT_WORD.containsMatchIn(lower) && (lower.contains("onto") || lower.contains("street") || lower.contains("road") || lower.contains("ave")) -> TurnDirection.RIGHT
            else -> TurnDirection.STRAIGHT
        }
    }

    private val SEGMENT_SPLIT = Regex("\\s*[\u2022\u00B7|]\\s*")

    /** Splits a navigation notification into the rows of the expanded card. */
    fun cardText(title: String, text: String): NavigationCardText {
        val segments = text.split(SEGMENT_SPLIT).map { it.trim() }.filter { it.isNotEmpty() }
        val (statSegments, detailSegments) = segments.partition { NavigationLocale.TRIP_STAT.containsMatchIn(it) || NavigationLocale.TRIP_DISTANCE.matches(it) }
        val eta = statSegments.joinToString(" \u2022 ")
        val detail = detailSegments.joinToString(" \u2022 ")

        val distanceSource = "$title $detail"
        val distMatch = NavigationLocale.ANY_DISTANCE.find(distanceSource)
        // "In 300 m" for Latin-script text; other scripts show the distance as the app wrote it ("через 300 м" stays unprefixed).
        val distance = when {
            distMatch == null -> "In 200 m"
            distMatch.value.all { it.code < 128 } -> "In ${distMatch.value}"
            else -> distMatch.value
        }

        val trimmedTitle = title.trim()
        val stripped = trimmedTitle.replace(NavigationLocale.LEADING_DISTANCE_PREFIX, "").trim()
        val instruction = when {
            stripped.isNotEmpty() -> stripped
            trimmedTitle.isNotEmpty() -> trimmedTitle
            else -> "Navigation"
        }
        return NavigationCardText(distance, instruction, detail, eta)
    }
}
