/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.session.MediaController
import android.os.Build

/**
 * Where the music is playing right now, for the media card's output chip. The decision is pure ([MediaOutputRules]);
 * [MediaOutputReader] only gathers the facts from Android.
 */
enum class MediaOutputKind { Phone, Bluetooth, Headphones, Usb, Hdmi, Cast }

/** [name] is the device's own name (a speaker, a pair of earbuds), or null when there is none worth showing. */
data class MediaOutput(val kind: MediaOutputKind, val name: String?) {
    companion object {
        val Phone = MediaOutput(MediaOutputKind.Phone, null)
    }
}

/** One connected audio output as the rules see it: an [AudioDeviceInfo] type id and its product name. */
data class OutputCandidate(val type: Int, val name: String?)

object MediaOutputRules {
    /** Chip text is cut here so a long device name cannot push the app badge off the row. */
    const val MAX_LABEL_CHARS = 18

    fun kindOf(type: Int): MediaOutputKind? = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> MediaOutputKind.Phone
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_HEARING_AID -> MediaOutputKind.Bluetooth
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> MediaOutputKind.Headphones
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> MediaOutputKind.Usb
        AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC, AudioDeviceInfo.TYPE_DOCK -> MediaOutputKind.Hdmi
        else -> if (type == TYPE_BLE_HEADSET || type == TYPE_BLE_SPEAKER || type == TYPE_BLE_BROADCAST) MediaOutputKind.Bluetooth else null
    }

    // AudioDeviceInfo.TYPE_BLE_* arrived in API 31 / 33; the numbers are fixed, so they are written out for minSdk 26.
    private const val TYPE_BLE_HEADSET = 26
    private const val TYPE_BLE_SPEAKER = 27
    private const val TYPE_BLE_BROADCAST = 30

    private fun rank(kind: MediaOutputKind): Int = when (kind) {
        MediaOutputKind.Bluetooth -> 0
        MediaOutputKind.Headphones -> 1
        MediaOutputKind.Usb -> 2
        MediaOutputKind.Hdmi -> 3
        MediaOutputKind.Phone -> 4
        MediaOutputKind.Cast -> 5
    }

    /**
     * The output to show. [remote] is a session playing on another device (cast, a TV): no local device is asked
     * then. With nothing recognisable the phone is shown.
     */
    fun resolve(remote: Boolean, remoteName: String?, candidates: List<OutputCandidate>): MediaOutput {
        if (remote) return MediaOutput(MediaOutputKind.Cast, cleanName(remoteName))
        val best = candidates
            .mapNotNull { c -> kindOf(c.type)?.let { it to c } }
            // Earlier candidates win a tie: the system lists the routed device first.
            .withIndex()
            .minWithOrNull(compareBy({ rank(it.value.first) }, { it.index }))
            ?.value ?: return MediaOutput.Phone
        val (kind, device) = best
        return when (kind) {
            MediaOutputKind.Phone -> MediaOutput.Phone
            // The 3.5 mm jack has no product name of its own: Android reports the driver's id there (\"h2w\"). Show the plain word.
            MediaOutputKind.Headphones -> MediaOutput(kind, null)
            else -> MediaOutput(kind, cleanName(device.name))
        }
    }

    /** The chip text: the device name, or the kind's plain word when it has no name. [phoneLabel] is "This phone". */
    fun label(
        output: MediaOutput,
        phoneLabel: String,
        castLabel: String,
        wiredLabel: String,
        headphonesLabel: String = wiredLabel
    ): String {
        val fallback = when (output.kind) {
            MediaOutputKind.Phone -> phoneLabel
            MediaOutputKind.Cast -> castLabel
            MediaOutputKind.Bluetooth -> "Bluetooth"
            MediaOutputKind.Headphones -> headphonesLabel
            MediaOutputKind.Usb, MediaOutputKind.Hdmi -> wiredLabel
        }
        val text = output.name ?: fallback
        return if (text.length <= MAX_LABEL_CHARS) text else text.take(MAX_LABEL_CHARS - 1).trimEnd() + "…"
    }

    /**
     * The name of the cast target. [controlId] is the session's volume control id (what a remote session reports),
     * [routes] are the router's (id, name) pairs.
     */
    fun remoteRouteName(controlId: String?, routes: List<Pair<String, String?>>): String? {
        val id = controlId?.takeIf { it.isNotEmpty() } ?: return null
        return cleanName(routes.firstOrNull { it.first == id }?.second)
    }

    private fun cleanName(raw: String?): String? = raw?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", true) }
}

object MediaOutputReader {
    /** Reads the current output for [controller]'s session (null controller = the phone's own audio). */
    fun read(context: Context, controller: MediaController?): MediaOutput {
        val info = controller?.playbackInfo
        if (info != null && info.playbackType == android.media.session.MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE) {
            return MediaOutputRules.resolve(remote = true, remoteName = castTargetName(context, info), candidates = emptyList())
        }
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return MediaOutput.Phone
        val devices: List<AudioDeviceInfo> = runCatchingLogged("MediaOutput", "reading the audio outputs failed") {
            routedDevices(audio, info?.audioAttributes) ?: audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        } ?: emptyList()
        val candidates = devices.map { OutputCandidate(it.type, it.productName?.toString()) }
        return MediaOutputRules.resolve(remote = false, remoteName = null, candidates = candidates)
    }

    private fun castTargetName(context: Context, info: MediaController.PlaybackInfo): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return runCatchingLogged("MediaOutput", "reading the cast target failed") {
            routerRouteName(context, info.volumeControlId)
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
    private fun routerRouteName(context: Context, controlId: String?): String? {
        val router = android.media.MediaRouter2.getInstance(context)
        val routes = router.routes.map { it.id to it.name?.toString() }
        val name = MediaOutputRules.remoteRouteName(controlId, routes)
        AppLogRecorder.i("MediaOutput", "cast target controlId=$controlId routes=${routes.size} name=$name")
        return name
    }

    private fun routedDevices(audio: AudioManager, attributes: AudioAttributes?): List<AudioDeviceInfo>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val attrs = attributes ?: AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
        return audio.getAudioDevicesForAttributes(attrs).takeIf { it.isNotEmpty() }
    }
}
