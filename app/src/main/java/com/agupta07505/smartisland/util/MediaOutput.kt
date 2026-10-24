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
import android.media.MediaRoute2Info
import android.media.MediaRouter2
import android.media.RouteDiscoveryPreference
import android.media.session.MediaController
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "MediaOutput"

/** How long a listening router gets to list the cast target before the lookup moves on. */
private const val ROUTE_WAIT_MS = 1200L

/**
 * Where the music plays right now, for the output chip. The decision is pure ([MediaOutputRules]), [MediaOutputReader]
 * only gathers facts from Android
 */
enum class MediaOutputKind { Phone, Bluetooth, Headphones, Usb, Hdmi, Cast }

/** What a cast target is, for its icon: a speaker, a TV, a computer. [Generic] when nothing says. */
enum class RemoteDevice { Generic, Speaker, Tv, Computer, Phone, Tablet, Car }

/** A cast target the router knows: its [name] and the route's [type] (`MediaRoute2Info.TYPE_*`), null when unknown. */
data class RemoteTarget(val name: String, val type: Int?)

/** [name] is the device's own name (a speaker, earbuds), null when none is worth showing. [device] only counts for [MediaOutputKind.Cast]. */
data class MediaOutput(val kind: MediaOutputKind, val name: String?, val device: RemoteDevice = RemoteDevice.Generic) {
    companion object {
        val Phone = MediaOutput(MediaOutputKind.Phone, null)
    }
}

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
        else -> if (type == TYPE_BLE_HEADSET || type == TYPE_BLE_SPEAKER
            || type == TYPE_BLE_BROADCAST) MediaOutputKind.Bluetooth else null
    }

    // AudioDeviceInfo.TYPE_BLE_* arrived in API 31 / 33; the numbers are fixed, so they are written out for minSdk 26
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
     * Output to show. [remote] is a session playing on another device (cast, a TV), no local device is asked then. With
     * nothing recognisable the phone is shown
     */
    fun resolve(
        remote: Boolean,
        remoteName: String?,
        candidates: List<OutputCandidate>,
        remoteType: Int? = null,
        remoteDevice: RemoteDevice? = null
    ): MediaOutput {
        if (remote) {
            return MediaOutput(
                MediaOutputKind.Cast, cleanName(remoteName), remoteDevice ?: remoteDeviceOf(remoteType)
            )
        }
        val best = candidates
            .mapNotNull { c -> kindOf(c.type)?.let { it to c } }
            // Earlier candidates win a tie: the system lists the routed device first
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

    /** What the route's type says the target is, the way the system's output switcher picks its icon. */
    fun remoteDeviceOf(routeType: Int?): RemoteDevice = when (routeType) {
        MediaRoute2Info.TYPE_REMOTE_TV -> RemoteDevice.Tv
        MediaRoute2Info.TYPE_REMOTE_SPEAKER -> RemoteDevice.Speaker
        MediaRoute2Info.TYPE_REMOTE_COMPUTER -> RemoteDevice.Computer
        MediaRoute2Info.TYPE_REMOTE_SMARTPHONE -> RemoteDevice.Phone
        MediaRoute2Info.TYPE_REMOTE_TABLET, MediaRoute2Info.TYPE_REMOTE_TABLET_DOCKED -> RemoteDevice.Tablet
        MediaRoute2Info.TYPE_REMOTE_CAR -> RemoteDevice.Car
        else -> RemoteDevice.Generic
    }

    private val ROUTE_TYPE = Regex("""\btype=([A-Za-z_]+)""")

    /** The type's word as `dumpsys media_router` prints a route (`REMOTE_TV`, `REMOTE_SPEAKER`...). */
    fun remoteDeviceOfTypeName(typeName: String?): RemoteDevice {
        val t = typeName?.uppercase() ?: return RemoteDevice.Generic
        return when {
            t.contains("TV") -> RemoteDevice.Tv
            t.contains("SPEAKER") || t.contains("AUDIO_VIDEO_RECEIVER") -> RemoteDevice.Speaker
            t.contains("COMPUTER") -> RemoteDevice.Computer
            t.contains("SMARTPHONE") -> RemoteDevice.Phone
            t.contains("TABLET") -> RemoteDevice.Tablet
            t.contains("CAR") -> RemoteDevice.Car
            else -> RemoteDevice.Generic
        }
    }

    /** Type of the route called [name] in the dump, or Generic when the dump does not print one. */
    fun remoteDeviceFromDump(dump: String, name: String?): RemoteDevice {
        if (name.isNullOrBlank()) return RemoteDevice.Generic
        for (line in dump.lineSequence()) {
            if (!line.contains("name=$name")) continue
            ROUTE_TYPE.find(line)?.groupValues?.get(1)?.let { return remoteDeviceOfTypeName(it) }
        }
        return RemoteDevice.Generic
    }

    /** Chip text: the device name, or the kind's plain word when it has no name. [phoneLabel] is "This phone". */
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
        // Desktop playback can be a Cast route. Show the desktop name when the route provides it.
        val text = output.name ?: fallback
        return if (text.length <= MAX_LABEL_CHARS) text else text.take(MAX_LABEL_CHARS - 1).trimEnd() + "…"
    }

    /**
     * Cast target's name. [controlId] is the session's volume control id, [routes] the router's (id, name) pairs. For
     * Spotify Connect the control id is a session id (`package/Service:session`) and the route is `package/Service:device`,
     * so when no id matches the route is taken from the same provider (the text before the last colon): the only one
     * it has, else the one in [connectedIds]
     */
    fun remoteRouteName(controlId: String?, routes: List<Pair<String, String?>>, connectedIds: Set<String> = emptySet()): String? {
        val id = controlId?.takeIf { it.isNotEmpty() } ?: return null
        routes.firstOrNull { it.first == id }?.let { match -> cleanName(match.second)?.let { return it } }
        val provider = id.substringBeforeLast(':', "").takeIf { it.isNotEmpty() } ?: return null
        val sameProvider = routes.filter { it.first.startsWith("$provider:") && cleanName(it.second) != null }
        val pick = sameProvider.singleOrNull()
            ?: sameProvider.filter { it.first in connectedIds }.singleOrNull()
            ?: return null
        return cleanName(pick.second)
    }

    private val ROUTE_ID = Regex("""\bid=([^,}\s]+)""")
    private val ROUTE_NAME = Regex("""\bname=([^,}]+)""")
    private val PIPE_ROW = Regex("""^\s*(\S+)\s+\|\s+(.+?)\s*$""")
    private val SELECTED = Regex("""selectedRoutes=\{([^}]*)\}""")

    /**
     * Cast target's name out of `dumpsys media_router`, for apps whose remote session (Spotify Connect) is not a route
     * this app's own MediaRouter2 can see. The system output switcher gets its chip name from the same data.
     * [controlId] is tried first as a route id, then the routes selected by a session that belongs to [packageName].
     * Null when the dump does not say, so the caller falls back to the plain word.
     */
    fun remoteNameFromDump(dump: String, packageName: String?, controlId: String?): String? {
        if (dump.isBlank()) return null
        val names = LinkedHashMap<String, String>()
        for (line in dump.lineSequence()) {
            // "id | name" rows, and MediaRoute2Info{ id=..., name=... } text
            val row = PIPE_ROW.find(line)
            if (row != null) {
                cleanName(row.groupValues[2])?.let { names.putIfAbsent(row.groupValues[1], it) }
                continue
            }
            if (!line.contains("name=")) continue
            val id = ROUTE_ID.find(line)?.groupValues?.get(1)?.trim() ?: continue
            val name = cleanName(ROUTE_NAME.find(line)?.groupValues?.get(1)) ?: continue
            names.putIfAbsent(id, name)
        }
        if (names.isEmpty()) return null
        remoteRouteName(controlId, names.map { it.key to it.value })?.let { return it }
        val pkg = packageName?.takeIf { it.isNotEmpty() } ?: return null
        for (line in dump.lineSequence()) {
            if (!line.contains(pkg) || !line.contains("selectedRoutes")) continue
            val ids = SELECTED.find(line)?.groupValues?.get(1)?.split(',')?.map { it.trim() }.orEmpty()
            ids.firstNotNullOfOrNull { names[it] }?.let { return it }
        }
        // One route of the app's own provider, e.g. the single Spotify Connect device the router knows
        val own = names.filter { it.key.startsWith("$pkg/") }
        return own.values.singleOrNull()
    }

    private fun cleanName(raw: String?): String? = raw?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", true) }
}

object MediaOutputReader {
    /** Reads the current output for [controller]'s session (null controller = the phone's own audio). */
    suspend fun read(context: Context, controller: MediaController?): MediaOutput {
        val info = controller?.playbackInfo
        if (info != null && info.playbackType == MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE) {
            val target = castTarget(context, info)
            if (target != null) {
                return MediaOutputRules.resolve(
                    remote = true, remoteName = target.name, candidates = emptyList(), remoteType = target.type
                )
            }
            val system = castTargetFromSystem(controller?.packageName, info)
            return MediaOutputRules.resolve(
                remote = true, remoteName = system?.first, candidates = emptyList(), remoteDevice = system?.second
            )
        }
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return MediaOutput.Phone
        val devices: List<AudioDeviceInfo> = runCatchingLogged(TAG, "reading the audio outputs failed") {
            routedDevices(audio, info?.audioAttributes) ?: audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        } ?: emptyList()
        val candidates = devices.map { OutputCandidate(it.type, it.productName?.toString()) }
        return MediaOutputRules.resolve(remote = false, remoteName = null, candidates = candidates)
    }

    /** Shizuku only: the system's own routing sessions name the device when the app's router cannot see it. */
    private suspend fun castTargetFromSystem(
        packageName: String?,
        info: MediaController.PlaybackInfo
    ): Pair<String, RemoteDevice>? {
        if (!ShizukuManager.hasPermission()) return null
        val dump = ShizukuManager.runShellForOutput("dumpsys media_router").getOrNull() ?: return null
        val controlId = volumeControlId(info)
        val name = MediaOutputRules.remoteNameFromDump(dump, packageName, controlId)
        if (name == null) {
            AppLogRecorder.d(TAG, "media_router dump had no cast target for $packageName (control id $controlId)")
            return null
        }
        return name to MediaOutputRules.remoteDeviceFromDump(dump, name)
    }

    private fun volumeControlId(info: MediaController.PlaybackInfo): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) info.volumeControlId else null

    private suspend fun castTarget(context: Context, info: MediaController.PlaybackInfo): RemoteTarget? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return runSuspendCatchingLogged(TAG, "reading the cast target failed") {
            routerRouteName(context, info.volumeControlId)
        }
    }

    /**
     * The session's volume control id is the route's id (`package/Service:device-id` for Spotify Connect). An app's
     * router lists routes only while the app listens for them, so a plain read is empty: listen for a moment, read
     * again, stop listening
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun routerRouteName(context: Context, controlId: String?): RemoteTarget? {
        val router = MediaRouter2.getInstance(context)
        fun lookup(): String? = MediaOutputRules.remoteRouteName(
            controlId,
            router.routes.map { it.id to it.name?.toString() },
            router.routes.filter { it.connectionState == MediaRoute2Info.CONNECTION_STATE_CONNECTED }.map { it.id }.toSet()
        )
        fun target(name: String?): RemoteTarget? = name?.let { n ->
            RemoteTarget(n, router.routes.firstOrNull { it.name?.toString()?.trim() == n }?.let { route ->
                // the route's type is Android 14 and later, older versions give the name only
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) route.type else null
            })
        }
        target(lookup())?.let { return it }
        if (controlId.isNullOrEmpty()) return null
        val found = CompletableDeferred<Unit>()
        val callback = object : MediaRouter2.RouteCallback() {
            override fun onRoutesAdded(routes: List<MediaRoute2Info>) {
                if (lookup() != null) found.complete(Unit)
            }

            override fun onRoutesChanged(routes: List<MediaRoute2Info>) {
                if (lookup() != null) found.complete(Unit)
            }
        }
        val preference = RouteDiscoveryPreference.Builder(
            listOf(
                MediaRoute2Info.FEATURE_REMOTE_PLAYBACK,
                MediaRoute2Info.FEATURE_LIVE_AUDIO,
                MediaRoute2Info.FEATURE_LIVE_VIDEO
            ),
            true
        ).build()
        router.registerRouteCallback(context.mainExecutor, callback, preference)
        try {
            withTimeoutOrNull(ROUTE_WAIT_MS) { found.await() }
            val name = lookup()
            if (name == null) {
                AppLogRecorder.d(TAG, "router lists ${router.routes.size} routes, none has id $controlId")
            }
            return target(name)
        } finally {
            router.unregisterRouteCallback(callback)
        }
    }

    private fun routedDevices(audio: AudioManager, attributes: AudioAttributes?): List<AudioDeviceInfo>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val attrs = attributes ?: AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
        return audio.getAudioDevicesForAttributes(attrs).takeIf { it.isNotEmpty() }
    }
}
