/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.alert

import com.agupta07505.smartisland.model.IslandNotification

/**
 * Where an alert came from. Internal tools (battery, Bluetooth, flashlight from the overlay service and
 * `SystemEventReceiver`, and the in-app demos) do not need notification access; everything else came from
 * the notification listener and must go when that access is lost (Rule_Book §7.1).
 */
object AlertSource {
    private val internalPrefixes = listOf("system_", "demo_")

    fun isInternal(notification: IslandNotification): Boolean =
        internalPrefixes.any { notification.key.startsWith(it) }

    /** Keys to flush from the repository when notification access is revoked or the listener disconnects. */
    fun listenerSourcedKeys(list: List<IslandNotification>): List<String> =
        list.filterNot(::isInternal).map { it.key }
}
