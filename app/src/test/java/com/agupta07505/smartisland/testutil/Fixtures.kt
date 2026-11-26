/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.testutil

import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction

/**
 * One place that builds the objects most tests need, so a new constructor parameter is a change here and not in every
 * test. Every default is a fixed value, never the clock. Fields a test does not name keep the production defaults,
 * set any other field with `copy`
 */
fun testCard(
    key: String = "k",
    mode: IslandMode = IslandMode.Notification,
    packageName: String = "pkg.$key",
    appName: String = key,
    title: String = key,
    text: String = "",
    timeMillis: Long = 1L,
    category: String? = null,
    silent: Boolean = false,
    actions: List<IslandNotificationAction> = emptyList()
): IslandNotification = IslandNotification(
    key = key,
    packageName = packageName,
    appName = appName,
    title = title,
    text = text,
    timeMillis = timeMillis,
    category = category,
    mode = mode,
    isSilent = silent,
    actionIntents = actions
)

/** A chat message card from the `com.chat` app */
fun chatCard(key: String, text: String = key): IslandNotification =
    testCard(key = key, packageName = "com.chat", appName = "Chat", text = text)

/** The music card, playing or paused */
fun musicCard(playing: Boolean): IslandNotification =
    testCard(
        key = "music", mode = IslandMode.Music, packageName = "com.music", appName = "Music",
        title = "Song", text = "Artist"
    ).copy(mediaIsPlaying = playing)

/** The phone call card: ringing offers Answer and Decline, an active call offers Hang up */
fun callCard(ringing: Boolean): IslandNotification =
    testCard(
        key = "call", mode = IslandMode.IncomingCall, packageName = "com.dialer", appName = "Phone",
        title = "Ann", timeMillis = 5L,
        actions = if (ringing) {
            listOf(IslandNotificationAction("Answer", null), IslandNotificationAction("Decline", null))
        } else {
            listOf(IslandNotificationAction("Hang up", null))
        }
    )

/** The production defaults, changed by [configure], for example `testSettings { copy(enabled = true) }` */
fun testSettings(configure: SmartIslandSettings.() -> SmartIslandSettings = { this }): SmartIslandSettings =
    SmartIslandSettings().configure()
