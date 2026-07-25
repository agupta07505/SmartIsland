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

data class LiveActivityInfo(
    val packageName: String,
    val etaText: String,
    val progressRatio: Float,
    val statusTitle: String,
    val subStatusText: String? = null,
    val stage: Int = 2,
    val totalStages: Int = 3
)

object LiveActivityParser {

    private val SUPPORTED_PACKAGES = setOf(
        "com.application.zomato",      // Zomato
        "in.swiggy.android",           // Swiggy & Instamart
        "com.grofers.customerapp",     // Blinkit
        "com.zepto.consumer",          // Zepto
        "com.ubercab",                 // Uber
        "com.rapido.passenger",        // Rapido
        "com.olacabs.customer",        // Ola
        "com.dunzo.user",              // Dunzo
        "com.Dominos"                  // Domino's
    )

    private val ETA_PATTERN = Pattern.compile("(\\d+)(?:\\s*-\\s*\\d+)?\\s*(?:mins?|minutes?|min|m)\\b", Pattern.CASE_INSENSITIVE)
    private val DISTANCE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:km|miles?|m)\\b", Pattern.CASE_INSENSITIVE)

    private val STATUS_STAGE_1 = listOf("placed", "accepted", "booked", "finding", "searching", "confirmed", "assigned")
    private val STATUS_STAGE_2 = listOf("preparing", "on the way", "heading", "en route", "picked up", "out for delivery", "driving", "dispatched")
    private val STATUS_STAGE_3 = listOf("arriving", "arrived", "reaching", "at location", "nearby", "delivered", "completed")

    fun isSupportedApp(packageName: String): Boolean {
        return packageName in SUPPORTED_PACKAGES
    }

    fun parse(sbn: StatusBarNotification): LiveActivityInfo? {
        val packageName = sbn.packageName
        if (!isSupportedApp(packageName)) return null

        val notification = sbn.notification ?: return null
        val extras = notification.extras ?: return null

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString().orEmpty()

        val combinedContent = "$title $text".lowercase()

        val progress = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)

        // 1. Extract ETA
        val etaMatcher = ETA_PATTERN.matcher("$title $text")
        val etaText = if (etaMatcher.find()) {
            val mins = etaMatcher.group(1)
            "$mins min"
        } else if (combinedContent.contains("arrived") || combinedContent.contains("reaching")) {
            "Arriving"
        } else if (progressMax > 0) {
            val remainingPct = (100 - (progress * 100 / progressMax)).coerceIn(0, 100)
            "$remainingPct%"
        } else {
            null
        }

        // Detect if this is a tracking notification vs a promo notification
        val isPromo = isPromoNotification(combinedContent)
        val isTrackingNotification = !isPromo && (
            etaText != null ||
            progressMax > 0 ||
            hasTrackingKeywords(combinedContent)
        )

        if (!isTrackingNotification) {
            return null
        }

        // 2. Determine Stage
        val stage = when {
            STATUS_STAGE_3.any { combinedContent.contains(it) } -> 3
            STATUS_STAGE_1.any { combinedContent.contains(it) } -> 1
            else -> 2
        }

        // 3. Calculate Progress Ratio (0.0f to 1.0f)
        val progressRatio = when {
            progressMax > 0 -> (progress.toFloat() / progressMax.toFloat()).coerceIn(0f, 1f)
            etaText != null && etaText.contains("min") -> {
                val mins = etaMatcher.group(1)?.toFloatOrNull() ?: 15f
                (1f - (mins / 30f)).coerceIn(0.15f, 0.95f)
            }
            stage == 1 -> 0.30f
            stage == 3 -> 0.90f
            else -> 0.65f
        }

        // 4. Extract Distance if present
        val distMatcher = DISTANCE_PATTERN.matcher("$title $text")
        val distanceText = if (distMatcher.find()) {
            distMatcher.group(0)
        } else null

        // 5. Construct Status and SubStatus
        val finalStatusTitle = if (title.isNotBlank()) title else "Order / Ride in progress"
        val subStatusText = when {
            distanceText != null -> "Distance: $distanceText • $text"
            text.isNotBlank() -> text
            else -> "Tracking in real-time"
        }

        return LiveActivityInfo(
            packageName = packageName,
            etaText = etaText ?: "Active",
            progressRatio = progressRatio,
            statusTitle = finalStatusTitle,
            subStatusText = subStatusText,
            stage = stage,
            totalStages = 3
        )
    }

    private fun isPromoNotification(content: String): Boolean {
        val promoKeywords = listOf("order now", "flat ", "% off", "off on", "discount", "coupon", "code ", "cashback", "sale", "craving")
        return promoKeywords.any { content.contains(it) }
    }

    private fun hasTrackingKeywords(content: String): Boolean {
        val trackingPhrases = listOf(
            "on the way", "en route", "out for delivery", "preparing", "dispatched", "picked up",
            "driver", "captain", "arriving", "arrived", "heading to", "your order", "order status",
            "order placed", "order confirmed", "order in progress", "ride status", "cab arriving", "auto arriving"
        )
        return trackingPhrases.any { content.contains(it) }
    }
}
