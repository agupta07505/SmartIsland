/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Short state word the wide pulse shows as the value of a connection or device-state card, like the demo (Connected,
 * Disconnected, Plugged in, Unplugged). Pure rule, the words are an enum so the model stays free of locale and the
 * UI maps each one to its string
 */
enum class StateWord { Connected, Disconnected, PluggedIn, Unplugged, On, Off }

object StateWords {
    fun of(card: IslandNotification): StateWord? =
        if (card.mode == IslandMode.Bluetooth) of(card.category) else null

    fun of(category: String?): StateWord? = when {
        category == null -> null
        category == "wifi" || category == "vpn" || category == "usb_connected" -> StateWord.Connected
        category == "wifi_disconnected" || category == "vpn_disconnected" || category == "usb_disconnected" ->
            StateWord.Disconnected
        category == "headphones_connected" -> StateWord.PluggedIn
        category == "headphones_disconnected" -> StateWord.Unplugged
        DeviceStates.isDeviceState(category) -> if (DeviceStates.isOff(category)) StateWord.Off else StateWord.On
        else -> null
    }

    /**
     * Whether the glyph of the card with [category] is drawn with a slash, as the demo draws it: USB and wired
     * headphones that went away, and a mode that ended (it has no "off" icon of its own). Wi-Fi lost keeps its red
     * Wi-Fi icon, VPN lost its key-off icon, Do Not Disturb off and airplane mode off their own icons, Bluetooth is
     * unchanged
     */
    fun slashed(category: String?): Boolean =
        category == "usb_disconnected" || category == "headphones_disconnected" ||
            (DeviceStates.eventOf(category) == IslandEvent.MODE && DeviceStates.isOff(category))

    private val TRAILING_STATE = mapOf(
        StateWord.Connected to trailing("connected"),
        StateWord.Disconnected to trailing("disconnected"),
        StateWord.PluggedIn to trailing("plugged in"),
        StateWord.Unplugged to trailing("unplugged")
    )

    private fun trailing(phrase: String) = Regex("[\\s\\-\u2013\u2014:\u00B7\u2022]*\\b$phrase\\s*$", RegexOption.IGNORE_CASE)

    /** The name of what connected or went away, without the state the word beside it already says, null for the others. */
    private fun plainName(event: IslandEvent?): String? = when (event) {
        IslandEvent.WIFI, IslandEvent.WIFI_DISCONNECTED -> "Wi-Fi"
        IslandEvent.VPN, IslandEvent.VPN_DISCONNECTED -> "VPN"
        IslandEvent.USB_CONNECTED, IslandEvent.USB_DISCONNECTED -> "USB"
        IslandEvent.HEADPHONES_CONNECTED, IslandEvent.HEADPHONES_DISCONNECTED -> "Headphone"
        else -> null
    }

    /**
     * The title the pulse of a card with [word] shows beside the glyph. The word is drawn at the other end of the
     * pulse, so a title that says it too ("VPN connected", or the event's label "USB in") would show it twice: the
     * state is taken off the title, and a title that was only the state, or none, gives the plain name of the event
     */
    fun pulseTitle(title: String, word: StateWord?, event: IslandEvent?): String {
        val trailing = TRAILING_STATE[word] ?: return title
        val base = title.ifBlank { plainName(event).orEmpty() }
        return base.replace(trailing, "").trim().ifEmpty { plainName(event).orEmpty() }
    }
}
