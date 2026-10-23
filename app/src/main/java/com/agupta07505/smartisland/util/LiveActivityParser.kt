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

    private val ETA_PATTERN = Pattern.compile("(\\d+)(?:\\s*-\\s*\\d+)?\\s*(?:mins?|minutes?|min|минут[аы]?|мин|хв|minutos?|minuti|minuten|minuut|dakika|dk|menit|phút|分钟|分鐘|分|분|دقيقة|دقائق|मिनट)(?![\\p{L}\\p{N}])", Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
    private val DISTANCE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:km|miles?|m)\\b", Pattern.CASE_INSENSITIVE)

    private val STATUS_STAGE_1 = listOf("placed", "accepted", "booked", "finding", "searching", "confirmed", "assigned",
        "принят", "подтвержд", "ищем", "pedido recibido", "confirmado", "commande confirmée", "bestellung bestätigt", "ordine confermato", "已接单", "已下单", "주문 접수")
    private val STATUS_STAGE_3 = listOf("arriving", "arrived", "reaching", "at location", "nearby", "delivered", "completed",
        "прибыл", "приехал", "доставлен", "llegando", "ha llegado", "entregado", "livré", "angekommen", "geliefert", "in arrivo", "consegnato", "已送达", "已到达", "司机已到", "配達完了", "배달 완료")

    fun getBrandColor(packageName: String?): Long {
        return AppPackageCatalog.brandColor(packageName) ?: 0xFF38BDF8L
    }

    fun isSupportedApp(packageName: String): Boolean {
        return AppPackageCatalog.isLiveActivityApp(packageName)
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

        val etaMatcher = ETA_PATTERN.matcher("$title $text")
        var extractedMins: Float? = null
        val etaText = if (etaMatcher.find()) {
            val minsStr = etaMatcher.group(1)
            extractedMins = minsStr?.toFloatOrNull()
            "$minsStr min"
        } else if (combinedContent.contains("arrived") || combinedContent.contains("reaching")) {
            "Arriving"
        } else if (progressMax > 0) {
            val remainingPct = (100 - (progress * 100 / progressMax)).coerceIn(0, 100)
            "$remainingPct%"
        } else {
            null
        }

        val isPromo = isPromoNotification(combinedContent)
        val isTrackingNotification = !isPromo && (
            etaText != null ||
            progressMax > 0 ||
            hasTrackingKeywords(combinedContent)
        )

        if (!isTrackingNotification) {
            return null
        }

        val stage = when {
            STATUS_STAGE_3.any { combinedContent.contains(it) } -> 3
            STATUS_STAGE_1.any { combinedContent.contains(it) } -> 1
            else -> 2
        }

        val progressRatio = when {
            progressMax > 0 -> (progress.toFloat() / progressMax.toFloat()).coerceIn(0f, 1f)
            extractedMins != null -> {
                (1f - (extractedMins / 30f)).coerceIn(0.15f, 0.95f)
            }
            stage == 1 -> 0.30f
            stage == 3 -> 0.90f
            else -> 0.65f
        }

        val distMatcher = DISTANCE_PATTERN.matcher("$title $text")
        val distanceText = if (distMatcher.find()) {
            distMatcher.group(0)
        } else null

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
            "order placed", "order confirmed", "order in progress", "ride status", "cab arriving", "auto arriving",
            "курьер", "водитель", "ваш заказ", "в пути", "готовится", "tu pedido", "repartidor", "tu conductor", "en camino", "seu pedido", "a caminho",
            "votre commande", "votre chauffeur", "livreur", "en route", "ihre bestellung", "ihr fahrer", "unterwegs", "il tuo ordine", "in consegna",
            "siparişiniz", "kurye", "yolda", "zamówienie", "w drodze", "pesanan anda", "dalam perjalanan", "đơn hàng", "đang giao", "您的订单", "骑手", "司机", "配送中",
            "주문", "배달 중", "طلبك", "في الطريق", "आपका ऑर्डर"
        )
        return trackingPhrases.any { content.contains(it) }
    }
}
