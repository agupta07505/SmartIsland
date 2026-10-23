/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

object PulsePeak {
    enum class ValueKind { Text, Clock, Bars, State, Icon, Reading, None }

    /** The picture a notification carries, so the message can lead with an icon: not read from the label's language. */
    enum class MediaKind { None, Picture, Sticker;
        val showsIcon: Boolean get() = this == Picture
    }

    fun mediaKindOf(stickerFlags: List<Boolean>): MediaKind = when {
        stickerFlags.isEmpty() -> MediaKind.None
        stickerFlags.first() -> MediaKind.Sticker
        else -> MediaKind.Picture
    }

    /**
     * [progress] is null for events without real progress, so no line is drawn. [message] is the text a notification
     * pulses beside the camera, empty for every other event; its value slot is [ValueKind.None]. [mediaKind] is set for
     * a notification with a picture or a sticker only; the message then starts with its icon. [stateWord] is set for a
     * connection or device-state card (value kind [ValueKind.State], [value] empty: the word is localized in the UI).
     * [artist] is a music card's artist, empty for every other event
     */
    data class Peak(
        val title: String,
        val value: String,
        val valueKind: ValueKind,
        val progress: Float?,
        val message: String = "",
        val mediaKind: MediaKind = MediaKind.None,
        val stateWord: StateWord? = null,
        val artist: String = ""
    ) {
        val sizedMessage: String get() = message.take(MESSAGE_MAX_CHARS)

        /** The glyph's own ring would repeat the line, so only an event without a line keeps it. */
        val showsGlyphRing: Boolean get() = progress == null

        val hasValue: Boolean get() = valueKind != ValueKind.None
    }

    const val MESSAGE_MAX_CHARS = 84

    fun shownChars(card: IslandNotification): Int {
        val title = CallEnds.pulseTitle(card)
            ?: card.title.ifBlank { IslandEvent.forNotification(card)?.shortLabel.orEmpty() }
        val beside = when (card.mode) {
            IslandMode.Notification, IslandMode.Music -> card.text.take(MESSAGE_MAX_CHARS)
            else -> ""
        }
        return title.length + beside.length
    }

    /**
     * A notification whose content the lock screen hides has no message or title to pulse, so the settled pill stays
     * as it is (glyph, dot) instead of being replaced by a pulse that draws only the glyph
     */
    fun keepsSettledLook(card: IslandNotification?, redacted: Boolean): Boolean =
        redacted && card?.mode == IslandMode.Notification

    /**
     * Events whose title comes from outside: a network, location, device, caller, song, file, an app's own text (a
     * clock app's timer label, a recorder's name), or a mode's own name (system's, maker's or user's, so it can be
     * localized or renamed).
     * `DND` and the other device states are not here, their titles come from [DeviceStates] and [DndModes.defaultName]
     */
    private val OUTSIDE_TITLE_EVENTS = setOf(
        IslandEvent.WIFI, IslandEvent.WIFI_DISCONNECTED, IslandEvent.VPN, IslandEvent.BLUETOOTH,
        IslandEvent.USB_CONNECTED, IslandEvent.USB_DISCONNECTED,
        IslandEvent.HEADPHONES_CONNECTED, IslandEvent.HEADPHONES_DISCONNECTED,
        IslandEvent.CALL, IslandEvent.MUSIC, IslandEvent.NAVIGATION, IslandEvent.LIVE_ACTIVITY,
        IslandEvent.DOWNLOAD, IslandEvent.UPLOAD, IslandEvent.BACKUP, IslandEvent.FILE_COPY_MOVE,
        IslandEvent.FILE_DELETE, IslandEvent.APP_INSTALL, IslandEvent.MODE,
        IslandEvent.TIMER, IslandEvent.TIMER_FINISHED, IslandEvent.STOPWATCH,
        IslandEvent.SCREEN_RECORDING, IslandEvent.AUDIO_RECORDING
    )

    fun hasCustomTitle(card: IslandNotification): Boolean {
        val event = IslandEvent.forNotification(card) ?: return false
        return event in OUTSIDE_TITLE_EVENTS && card.title.isNotBlank() && card.title !in FallbackTitles.of(event)
    }

    const val LINE_HEIGHT_DP = 2f

    const val LINE_EDGE_GAP_DP = 1.5f

    /**
     * The strip the pill grows by for its line: none. The line sits on the settled pill's own edge, so the pill keeps
     * its height and the glyphs and text keep their place
     */
    const val LINE_BAND_DP = 0f

    fun drawsLine(card: IslandNotification?, nowMs: Long): Boolean = card != null && of(card, nowMs).progress != null

    fun hidesGlyphRing(card: IslandNotification?, nowMs: Long): Boolean = drawsLine(card, nowMs)

    fun extraHeightDp(card: IslandNotification?, nowMs: Long): Float = if (drawsLine(card, nowMs)) LINE_BAND_DP else 0f

    fun of(card: IslandNotification, nowMs: Long): Peak {
        val event = IslandEvent.forNotification(card)
        val word = StateWords.of(card)
        val title = CallEnds.pulseTitle(card)
            ?: if (card.mode == IslandMode.Flashlight) event?.shortLabel.orEmpty().ifBlank { "Flashlight" }
            else StateWords.pulseTitle(card.title, word, event).ifBlank { event?.shortLabel.orEmpty() }
        val (kind, value) = if (word != null) ValueKind.State to "" else valueOf(card)
        val message = if (card.mode == IslandMode.Notification) card.text else ""
        val artist = if (card.mode == IslandMode.Music) card.text else ""
        val media = if (card.mode == IslandMode.Notification) mediaKindOf(card.media.filter { it.inLatestMessage }.map { it.isSticker }) else MediaKind.None
        return Peak(title, value, kind, progressOf(card, event, nowMs), message, media, word, artist)
    }

    private fun valueOf(card: IslandNotification): Pair<ValueKind, String> = when (card.mode) {
        IslandMode.Music -> ValueKind.Bars to ""
        IslandMode.Notification -> ValueKind.None to ""
        IslandMode.Timer, IslandMode.Stopwatch, IslandMode.ScreenRecording, IslandMode.AudioRecording ->
            ValueKind.Clock to ""
        IslandMode.CallEnded -> ValueKind.Text to CallEnds.format(card.callEnd?.durationMs ?: 0L)
        IslandMode.IncomingCall ->
            if (card.isCallRinging || card.isCallConnecting) {
                ValueKind.Text to card.callStatusText.ifBlank { "Ringing..." }
            } else {
                ValueKind.Clock to ""
            }
        IslandMode.DownloadUpload ->
            ValueKind.Text to (percentOf(card)?.let { "$it%" } ?: card.text)
        IslandMode.Navigation -> ValueKind.Text to PulseValueText.distance(card.title, card.text)
        IslandMode.LiveActivity -> ValueKind.Text to PulseValueText.eta(card.title, card.text)
        IslandMode.Flashlight -> ValueKind.Text to "ON"
        IslandMode.Hotspot ->
            ValueKind.Text to PulseValueText.hotspot(HotspotText.parseDeviceCountOrNull(card.title, card.text))
        IslandMode.Bluetooth -> bluetoothLevel(card)?.let { ValueKind.Reading to "$it%" } ?: (ValueKind.Icon to "")
        else -> ValueKind.Text to card.text
    }

    fun hasProgressLine(event: IslandEvent?): Boolean = when (event) {
        IslandEvent.PLUGGED_IN, IslandEvent.PLUGGED_OUT, IslandEvent.FULLY_CHARGED, IslandEvent.LOW_BATTERY,
        IslandEvent.BLUETOOTH, IslandEvent.MUSIC, IslandEvent.NAVIGATION, IslandEvent.LIVE_ACTIVITY,
        IslandEvent.DOWNLOAD, IslandEvent.UPLOAD, IslandEvent.BACKUP, IslandEvent.FILE_COPY_MOVE,
        IslandEvent.FILE_DELETE, IslandEvent.APP_INSTALL, IslandEvent.TIMER -> true
        else -> false
    }

    private fun progressOf(card: IslandNotification, event: IslandEvent?, nowMs: Long): Float? {
        if (!hasProgressLine(event)) return null
        return when (card.mode) {
            IslandMode.Battery -> percentOf(card)?.let { it / 100f }
            IslandMode.Bluetooth -> bluetoothLevel(card)?.let { it / 100f }
            IslandMode.Music -> {
                val duration = card.mediaDurationMs ?: return null
                val position = card.mediaPositionMs ?: return null
                if (duration > 0L) (position.toFloat() / duration).coerceIn(0f, 1f) else null
            }
            IslandMode.Timer -> TimerRings.fraction(card, nowMs = nowMs)
            IslandMode.Navigation -> null
            else -> percentOf(card)?.let { it / 100f }
        }
    }

    private fun percentOf(card: IslandNotification): Int? {
        if (card.progressMax > 0) return (card.progress * 100 / card.progressMax).coerceIn(0, 100)
        return PERCENT.find(card.text)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 100)
    }

    private fun bluetoothLevel(card: IslandNotification): Int? =
        if (card.progress in 1..100 && card.progressMax == 100) card.progress else percentOf(card)

    private val PERCENT = Regex("(\\d{1,3})%")
}
