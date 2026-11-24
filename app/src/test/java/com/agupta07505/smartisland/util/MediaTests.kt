/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.media.AudioDeviceInfo
import com.agupta07505.smartisland.data.SmartIslandNotificationRepository
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkPaletteTest {
    private fun argb(r: Int, g: Int, b: Int, a: Int = 255) = (a shl 24) or (r shl 16) or (g shl 8) or b

    private fun fill(count: Int, colour: Int) = IntArray(count) { colour }

    @Test
    fun artworkHue() {
        val tint = ArtworkPalette.dominantTint(fill(100, argb(220, 30, 40)))
        assertNotNull(tint)
        val redHue = tint!!.hue
        assertTrue("hue was $redHue", redHue < 15f || redHue > 345f)
        assertTrue(tint.saturation > 0.6f)

        val pixels = fill(40, argb(230, 20, 60)) + fill(40, argb(230, 20, 10)) + fill(20, argb(30, 80, 220))
        val wrappedHue = ArtworkPalette.dominantTint(pixels)!!.hue
        assertTrue("hue was $wrappedHue", wrappedHue < 20f || wrappedHue > 340f)

        assertNull(ArtworkPalette.dominantTint(IntArray(0)))

        val vivid = ArtworkPalette.dominantTint(fill(100, argb(120, 230, 40)))!!
        val dark = ArtworkPalette.dominantTint(fill(100, argb(70, 35, 25)))!!
        assertTrue("vivid ${vivid.chroma} dark ${dark.chroma}", vivid.chroma > dark.chroma + 20f)
        assertEquals(134f, vivid.labHue, 25f)
    }

    @Test
    fun artworkTintSelection() {
        val dominantPixels = fill(70, argb(30, 60, 220)) + fill(30, argb(220, 30, 40))
        val tint = ArtworkPalette.dominantTint(dominantPixels)!!
        assertEquals(230f, tint.hue, 15f)

        val patchPixels = fill(95, argb(40, 200, 60)) + fill(5, argb(255, 0, 0))
        assertEquals(125f, ArtworkPalette.dominantTint(patchPixels)!!.hue, 15f)

        val neutralPixels = fill(40, argb(0, 0, 0)) + fill(40, argb(255, 255, 255)) + fill(40, argb(128, 128, 128))
        assertNull(ArtworkPalette.dominantTint(neutralPixels))

        val darkPixels = fill(90, argb(10, 0, 0)) + fill(10, argb(30, 90, 220))
        // 10 % of the picture is blue, the rest is too dark to carry a colour: blue wins
        assertEquals(220f, ArtworkPalette.dominantTint(darkPixels)!!.hue, 15f)

        val transparentPixels = fill(100, argb(255, 0, 0, a = 0))
        assertNull(ArtworkPalette.dominantTint(transparentPixels))
    }
}

class AudioRecordingRulesTest {
    @Test
    fun audioRecordingNeedsRecorder() {
        assertTrue(OemDeviceRules.isAudioRecording("com.google.android.apps.recorder", "Recorder"))
        assertTrue(OemDeviceRules.isAudioRecording("com.sec.android.app.voicenote", "Voice Recorder"))
        assertTrue(OemDeviceRules.isAudioRecording("com.miui.soundrecorder", "Recording"))
        assertTrue(OemDeviceRules.isAudioRecording("com.some.app", "Voice recording in progress"))
        assertFalse(OemDeviceRules.isAudioRecording("com.sec.android.app.screenrecorder", "Recording screen"))
        assertFalse(OemDeviceRules.isAudioRecording("com.miui.screenrecorder", "Screen Recorder"))
        assertFalse(OemDeviceRules.isAudioRecording("com.vivo.videocapture", "Video capture"))
        assertFalse(OemDeviceRules.isAudioRecording("com.some.app", "Screen recording, voice recorder on"))
        assertFalse(OemDeviceRules.isAudioRecording("com.whatsapp", "New message"))
        assertFalse(OemDeviceRules.isAudioRecording("com.google.android.deskclock", "Timer"))
    }


}

class LabColorTest {
    private fun hex(rgb: FloatArray): String =
        "#%02x%02x%02x".format(Math.round(rgb[0] * 255f), Math.round(rgb[1] * 255f), Math.round(rgb[2] * 255f))

    private fun assertClose(expected: String, actual: FloatArray, tol: Int = 2) {
        for (i in 0..2) {
            val e = expected.substring(1 + 2 * i, 3 + 2 * i).toInt(16)
            val a = Math.round(actual[i] * 255f)
            assertTrue("$expected vs ${hex(actual)}", abs(e - a) <= tol)
        }
    }

    @Test
    fun paletteConversion() {
        // reference values from the notification shade: (chroma, hue) of the container at lightness 90
        assertClose("#c2e8ff", LabColor.lchToSrgb(90.0, 16.9, 244.0))
        assertClose("#a0f880", LabColor.lchToSrgb(90.0, 68.6, 134.0))
        assertClose("#dae5e1", LabColor.lchToSrgb(90.0, 4.5, 171.0))

        for (hue in listOf(0.0, 60.0, 134.0, 200.0, 280.0)) {
            val highChromaRgb = LabColor.lchToSrgb(90.0, 130.0, hue)
            assertEquals("hue $hue", 90.0, LabColor.lightnessOf(highChromaRgb), 1.5)
        }

        val lab = LabColor.fromSrgb(0xA0, 0xF8, 0x80)
        val roundTripRgb = LabColor.lchToSrgb(lab[0], LabColor.chromaOf(lab[1], lab[2]), LabColor.hueOf(lab[1], lab[2]))
        assertClose("#a0f880", roundTripRgb, 1)
    }

    @Test
    fun greyHasNoChroma() {
        val lab = LabColor.fromSrgb(128, 128, 128)
        assertTrue(LabColor.chromaOf(lab[1], lab[2]) < 0.5)
    }
}

class MediaArtCoverTest {
    @Test
    fun noRememberedCoverMeansNothingToHold() {
        assertNull(MediaArt.lastGood("never-seen-card", null))
    }

    @Test
    fun quickNoPictureNoFallbackIcon() {
        // A card with neither a large icon nor an app icon has nothing to draw, so it is not the app-icon stand-in
        val n = IslandNotification(key = "k1", packageName = "p", appName = "App", title = "t", text = "x", timeMillis = 0L)
        assertTrue(!MediaArt.quick(n).isFallbackIcon)
    }
}

class MediaArtTest {

    private fun music(title: String, text: String = "Artist") = IslandNotification(
        key = "music", packageName = "pkg.music", appName = "Music", title = title, text = text,
        mode = IslandMode.Music, timeMillis = 1L, mediaIsPlaying = true
    )

    @Test
    fun artworkCacheIdentity() {
        val ticking = music("Song A")
        val ticked = ticking.copy(mediaPositionMs = 42_000L)
        assertEquals(MediaArt.keyFor(ticking, 24f), MediaArt.keyFor(ticked, 24f))

        val changed = music("Song A")
        assertNotEquals(MediaArt.keyFor(changed, 24f), MediaArt.keyFor(music("Song B"), 24f))
        assertNotEquals(MediaArt.keyFor(changed, 24f), MediaArt.keyFor(changed, 8f))
        assertNotEquals(MediaArt.keyFor(changed, 24f), MediaArt.keyFor(changed, null))
    }
}

class MediaCardPatchTest {
    private fun card(title: String = "Old song", text: String = "Old artist") = IslandNotification(
        key = "music", packageName = "com.player", appName = "Player", title = title, text = text,
        timeMillis = 1L, mode = IslandMode.Music, mediaPositionMs = 5_000L, mediaDurationMs = 200_000L, mediaIsPlaying = true
    )

    private fun snap(title: String? = "New song", artist: String? = "New artist", playing: Boolean = true) =
        MediaSnapshot("com.player", title, artist, null, 180_000L, 0L, playing)

    @Test
    fun mediaPatchFields() {
        val trackOut = MediaCardPatch.apply(card(), snap())
        assertEquals("New song", trackOut.title)
        assertEquals("New artist", trackOut.text)
        assertEquals(180_000L, trackOut.mediaDurationMs)
        assertEquals(0L, trackOut.mediaPositionMs)

        val missingArtistOut = MediaCardPatch.apply(card(), snap(artist = null))
        assertEquals("New song", missingArtistOut.title)
        assertEquals("", missingArtistOut.text)

        val playbackOut = MediaCardPatch.apply(card(title = "Same", text = "Same artist"), snap(title = "Same", artist = null, playing = false))
        assertEquals("Same", playbackOut.title)
        assertEquals("Same artist", playbackOut.text)
        assertFalse(playbackOut.mediaIsPlaying)

        val repo = SmartIslandNotificationRepository()
        repo.postNotification(card())
        repo.updateNotification("music") { MediaCardPatch.apply(it, snap()) }
        assertEquals("New song", repo.notifications.value.single().title)
        repo.updateNotification("missing") { it.copy(title = "x") }
        assertEquals(1, repo.notifications.value.size)
    }

    @Test
    fun onlyPackageMusicTargeted() {
        val s = snap()
        assertTrue(MediaCardPatch.appliesTo(card(), s))
        assertFalse(MediaCardPatch.appliesTo(card().copy(packageName = "other"), s))
        assertFalse(MediaCardPatch.appliesTo(card().copy(mode = IslandMode.Notification), s))
    }

    @Test
    fun noChangeKeepsSameList() {
        val repo = SmartIslandNotificationRepository()
        repo.postNotification(card())
        val before = repo.notifications.value
        repo.updateNotification("music") { it }
        assertSame(before, repo.notifications.value)
        assertNull(repo.notifications.value.single().largeIcon)
    }
}

class MediaOutputSwitcherTest {
    @Test
    fun broadcastTargets() {
        assertEquals(
            "am broadcast -a com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG -p com.android.systemui --es package_name com.spotify.music 2>&1",
            MediaOutputSwitcher.broadcastCommand("com.spotify.music")
        )

        for (text in listOf("com.a; rm -rf /", "com.a b", "nodots", "", "com.a$(id)")) {
            assertNull(text, MediaOutputSwitcher.broadcastCommand(text))
        }
    }

    @Test
    fun successNeedsBroadcastNoThrow() {
        assertTrue(MediaOutputSwitcher.broadcastSucceeded("Broadcasting: Intent { ... }\nBroadcast completed: result=0"))
        assertFalse(MediaOutputSwitcher.broadcastSucceeded("java.lang.SecurityException: Permission Denial"))
        assertFalse(MediaOutputSwitcher.broadcastSucceeded(""))
    }

    @Test
    fun systemVariantHasNoPackage() {
        assertEquals(
            "am broadcast -a com.android.systemui.action.LAUNCH_SYSTEM_MEDIA_OUTPUT_DIALOG -p com.android.systemui 2>&1",
            MediaOutputSwitcher.systemBroadcastCommand()
        )
    }
}

class MediaOutputTest {
    private val speaker = OutputCandidate(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, "Pixel 8")
    private val buds = OutputCandidate(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "  Galaxy Buds  ")

    private fun label(o: MediaOutput) = MediaOutputRules.label(o, "This phone", "Cast", "Wired", "Wired headphone")

    @Test
    fun localAudioRoutes() {
        assertEquals(MediaOutput.Phone, MediaOutputRules.resolve(false, null, emptyList()))
        assertEquals(MediaOutput.Phone, MediaOutputRules.resolve(false, null, listOf(OutputCandidate(AudioDeviceInfo.TYPE_TELEPHONY, "x"))))

        val speakerOut = MediaOutputRules.resolve(false, null, listOf(speaker))
        assertEquals(MediaOutputKind.Phone, speakerOut.kind)
        assertEquals("This phone", label(speakerOut))

        val pairedOut = MediaOutputRules.resolve(false, null, listOf(speaker, buds))
        assertEquals(MediaOutputKind.Bluetooth, pairedOut.kind)
        assertEquals("Galaxy Buds", pairedOut.name)

        val a = OutputCandidate(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "First")
        val b = OutputCandidate(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "Second")
        assertEquals("First", MediaOutputRules.resolve(false, null, listOf(a, b)).name)

        val jack = OutputCandidate(AudioDeviceInfo.TYPE_WIRED_HEADSET, "h2w")
        val wiredOut = MediaOutputRules.resolve(false, null, listOf(jack))
        assertEquals(MediaOutputKind.Headphones, wiredOut.kind)
        assertEquals("Wired headphone", label(wiredOut))
    }

    @Test
    fun outputRoutePrecedence() {
        val wired = OutputCandidate(AudioDeviceInfo.TYPE_WIRED_HEADPHONES, null)
        val usb = OutputCandidate(AudioDeviceInfo.TYPE_USB_HEADSET, "DAC")
        val hdmi = OutputCandidate(AudioDeviceInfo.TYPE_HDMI, "TV")
        assertEquals(MediaOutputKind.Headphones, MediaOutputRules.resolve(false, null, listOf(hdmi, usb, wired)).kind)
        assertEquals(MediaOutputKind.Usb, MediaOutputRules.resolve(false, null, listOf(hdmi, usb)).kind)
        assertEquals(MediaOutputKind.Bluetooth, MediaOutputRules.resolve(false, null, listOf(wired, buds)).kind)

        val out = MediaOutputRules.resolve(true, null, listOf(buds))
        assertEquals(MediaOutputKind.Cast, out.kind)
        assertEquals("Cast", label(out))
        assertEquals("Living room", MediaOutputRules.resolve(true, "Living room", emptyList()).name)

        assertEquals(null, MediaOutputRules.resolve(false, null, listOf(OutputCandidate(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "  "))).name)
        assertEquals(null, MediaOutputRules.resolve(false, null, listOf(OutputCandidate(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "null"))).name)
    }

    @Test
    fun bleDevicesCountAsBluetooth() {
        assertEquals(MediaOutputKind.Bluetooth, MediaOutputRules.kindOf(26))
        assertEquals(MediaOutputKind.Bluetooth, MediaOutputRules.kindOf(27))
    }

    @Test
    fun labelFallbackAndCut() {
        assertEquals("Bluetooth", label(MediaOutput(MediaOutputKind.Bluetooth, null)))
        assertEquals("Wired headphone", label(MediaOutput(MediaOutputKind.Headphones, null)))
        assertEquals("Wired", label(MediaOutput(MediaOutputKind.Usb, null)))
        val cut = label(MediaOutput(MediaOutputKind.Bluetooth, "A very long speaker name indeed"))
        assertEquals(MediaOutputRules.MAX_LABEL_CHARS, cut.length)
        assertEquals('…', cut.last())
    }

    @Test
    fun castRouteNames() {
        val matchedRoutes = listOf("a" to "Living room TV", "b" to "Kitchen speaker")
        assertEquals("Kitchen speaker", MediaOutputRules.remoteRouteName("b", matchedRoutes))

        val missingRoutes = listOf("a" to "Living room TV")
        assertEquals(null, MediaOutputRules.remoteRouteName("zzz", missingRoutes))
        assertEquals(null, MediaOutputRules.remoteRouteName("", missingRoutes))
        assertEquals(null, MediaOutputRules.remoteRouteName(null, missingRoutes))
        assertEquals(null, MediaOutputRules.remoteRouteName("a", listOf("a" to "  ")))

        val providerRoutes = listOf("$spotifyRoute:3289ac07" to "DESKTOP-K005R01", "other/Svc:1" to "Kitchen")
        assertEquals("DESKTOP-K005R01", MediaOutputRules.remoteRouteName("$spotifyRoute:c01808a0-e1ec", providerRoutes))

        val connectedRoutes = listOf("$spotifyRoute:a" to "Desktop", "$spotifyRoute:b" to "Speaker")
        assertEquals(null, MediaOutputRules.remoteRouteName("$spotifyRoute:sess", connectedRoutes))
        assertEquals("Speaker", MediaOutputRules.remoteRouteName("$spotifyRoute:sess", connectedRoutes, setOf("$spotifyRoute:b")))
    }

    private val dump = """
        MediaRouter2ServiceImpl
          MediaRoute2Info{ id=route-desktop, name=DESKTOP-K005R01, features=[], type=REMOTE_COMPUTER }
          MediaRoute2Info{ id=route-phone, name=Phone speaker, features=[], type=BUILTIN_SPEAKER }
          RoutingSessionInfo{ sessionId=s1, clientPackageName=com.spotify.music, selectedRoutes={route-desktop} }
          RoutingSessionInfo{ sessionId=s2, clientPackageName=com.other, selectedRoutes={route-phone} }
    """.trimIndent()

    @Test
    fun remoteOutputNames() {
        assertEquals("DESKTOP-K005R01", MediaOutputRules.remoteNameFromDump(dump, "com.spotify.music", null))
        assertEquals("Phone speaker", MediaOutputRules.remoteNameFromDump(dump, "com.other", "nope"))

        assertEquals("Phone speaker", MediaOutputRules.remoteNameFromDump(dump, "com.spotify.music", "route-phone"))
        assertEquals(null, MediaOutputRules.remoteNameFromDump(dump, "com.unknown", null))
        assertEquals(null, MediaOutputRules.remoteNameFromDump("", "com.spotify.music", null))

        val line = "          $spotifyRoute:3289ac07cc8ee1f9 | DESKTOP-K005R01"
        assertEquals("DESKTOP-K005R01", MediaOutputRules.remoteNameFromDump(line, "com.spotify.music",
            "$spotifyRoute:c01808a0-e1ec-449f"))
    }

    private val spotifyRoute = "com.spotify.music/com.spotify.connect.mediarouteproviderapi.SpotifyMediaRouteProviderService"
}

class MediaTransportDetectorTest {
    @Test
    fun transportPlayersVsDownloads() {
        assertTrue(MediaTransportDetector.hasTransportButtons(listOf("Previous", "Pause", "Next", "Stop")))
        assertFalse(MediaTransportDetector.hasTransportButtons(listOf("Pause", "Cancel")))
        assertFalse(MediaTransportDetector.hasTransportButtons(emptyList()))
    }

}
