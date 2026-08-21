/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.notification.StatusBarNotification

enum class OemDeviceType(val displayName: String, val brandKeywords: List<String>) {
    AUTO("Auto-Detect (Current Device)", emptyList()),
    SAMSUNG("Samsung Galaxy (One UI)", listOf("samsung", "sec")),
    XIAOMI_REDMI_POCO("Xiaomi / Redmi / Poco (MIUI & HyperOS)", listOf("xiaomi", "redmi", "poco", "blackshark")),
    VIVO_IQOO("Vivo / iQOO (Funtouch OS & OriginOS)", listOf("vivo", "iqoo", "bbk")),
    REALME_OPPO_ONEPLUS("Realme / OPPO / OnePlus (ColorOS / Realme UI / OxygenOS)", listOf("oppo", "realme", "oneplus", "oplus", "heytap")),
    GOOGLE_PIXEL("Google Pixel & Stock Android", listOf("google", "pixel", "aosp")),
    HUAWEI_HONOR("Huawei / Honor (EMUI & MagicOS)", listOf("huawei", "honor")),
    MOTOROLA("Motorola / Lenovo (My UX)", listOf("motorola", "lenovo", "moto")),
    TRANSSION("Tecno / Infinix / Itel (XOS & HiOS)", listOf("transsion", "infinix", "tecno", "itel")),
    GENERIC("Generic Android", emptyList())
}

object OemDeviceRules {

    private val GENERIC_SCREEN_RECORDER_PACKAGES = setOf(
        "com.google.android.apps.recorder",
        "com.hecorat.screenrecorder.free",
        "com.kimcy929.screenrecorder",
        "videoeditor.videorecorder.screenrecorder",
        "com.rsupport.mvagent",
        "com.apowersoft.screenrecord",
        "com.nll.screenrecorder",
        "com.tmobile.screenrecorder",
        "com.orpheusdroid.screenrecorder"
    )

    private val SAMSUNG_SCREEN_RECORDER_PACKAGES = setOf(
        "com.sec.android.app.screenrecorder",
        "com.samsung.android.app.screenrecorder",
        "com.sec.android.app.smartcapture",
        "com.samsung.android.app.smartcapture",
        "com.sec.android.app.voicenote",
        "com.samsung.android.app.soundrecorder",
        "com.samsung.android.voicenote"
    )

    private val XIAOMI_SCREEN_RECORDER_PACKAGES = setOf(
        "com.miui.screenrecorder",
        "com.miui.soundrecorder",
        "com.android.soundrecorder"
    )

    private val VIVO_SCREEN_RECORDER_PACKAGES = setOf(
        "com.vivo.screenrecorder",
        "com.vivo.videocapture",
        "com.vivo.screenrecording",
        "com.vivo.easyshare",
        "com.iqoo.screenrecorder",
        "com.vivo.smartshot",
        "com.android.bbksoundrecorder",
        "com.vivo.soundrecorder",
        "com.bbk.record"
    )

    private val REALME_OPPO_SCREEN_RECORDER_PACKAGES = setOf(
        "com.oplus.screenrecorder",
        "com.coloros.screenrecorder",
        "com.realme.screenrecorder",
        "com.oneplus.screenrecorder",
        "com.heytap.screenrecorder",
        "com.coloros.soundrecorder",
        "com.oplus.soundrecorder",
        "com.realme.soundrecorder",
        "com.oneplus.soundrecorder",
        "com.heytap.soundrecorder"
    )

    private val HUAWEI_SCREEN_RECORDER_PACKAGES = setOf(
        "com.huawei.screenrecorder",
        "com.hihonor.screenrecorder",
        "com.huawei.soundrecorder",
        "com.hihonor.soundrecorder"
    )

    private val MOTOROLA_SCREEN_RECORDER_PACKAGES = setOf(
        "com.motorola.screenrecord",
        "com.motorola.mobiledesktop.screenrecorder",
        "com.motorola.soundrecorder"
    )

    private val TRANSSION_SCREEN_RECORDER_PACKAGES = setOf(
        "com.transsion.screenrecorder",
        "com.transsion.recorder",
        "com.transsion.soundrecorder"
    )

    private val ALL_OEM_RECORDER_PACKAGES = GENERIC_SCREEN_RECORDER_PACKAGES +
        SAMSUNG_SCREEN_RECORDER_PACKAGES +
        XIAOMI_SCREEN_RECORDER_PACKAGES +
        VIVO_SCREEN_RECORDER_PACKAGES +
        REALME_OPPO_SCREEN_RECORDER_PACKAGES +
        HUAWEI_SCREEN_RECORDER_PACKAGES +
        MOTOROLA_SCREEN_RECORDER_PACKAGES +
        TRANSSION_SCREEN_RECORDER_PACKAGES

    private val SCREEN_RECORDING_KEYWORDS = listOf(
        "screen recording", "recording screen", "screen recorder", "screen record",
        "recording file", "record screen", "recording audio", "voice recording",
        "smart capture", "s-capture", "capturing screen", "capture screen",
        "audio recording", "recording call", "call recording", "sound recorder",
        "recording in progress", "recording active", "voice recorder", "audio recorder"
    )

    private val SAMSUNG_INCALL_PACKAGES = setOf(
        "com.samsung.android.incallui",
        "com.samsung.android.dialer",
        "com.sec.phone",
        "com.samsung.android.app.telephonyui"
    )

    private val XIAOMI_INCALL_PACKAGES = setOf(
        "com.android.incallui",
        "com.google.android.dialer",
        "com.miui.voip",
        "com.miui.incallui"
    )

    private val VIVO_INCALL_PACKAGES = setOf(
        "com.android.bbk.incallui",
        "com.vivo.incallui"
    )

    private val REALME_OPPO_INCALL_PACKAGES = setOf(
        "com.oplus.incallui",
        "com.coloros.incallui",
        "com.oneplus.dialer",
        "com.oplus.dialer"
    )

    private val HUAWEI_INCALL_PACKAGES = setOf(
        "com.huawei.android.incallui",
        "com.hihonor.android.incallui"
    )

    private val ALL_OEM_INCALL_PACKAGES = SAMSUNG_INCALL_PACKAGES +
        XIAOMI_INCALL_PACKAGES +
        VIVO_INCALL_PACKAGES +
        REALME_OPPO_INCALL_PACKAGES +
        HUAWEI_INCALL_PACKAGES +
        setOf("com.google.android.dialer", "com.android.dialer", "com.android.server.telecom", "com.android.phone")

    private val HOTSPOT_PACKAGES = setOf(
        "com.samsung.android.app.mobilehotspot",
        "com.samsung.android.server.wifi.mobilehotspot",
        "com.miui.securitycenter",
        "com.vivo.easyshare",
        "com.google.android.tethering.entitlement"
    )

    private val HOTSPOT_KEYWORDS = listOf(
        "hotspot", "tethering", "portable hotspot", "mobile hotspot",
        "wifi hotspot", "usb tethering", "bluetooth tethering"
    )

    fun detectCurrentDevice(): OemDeviceType {
        val brand = (runCatching { Build.BRAND }.getOrNull() ?: "").lowercase()
        val manufacturer = (runCatching { Build.MANUFACTURER }.getOrNull() ?: "").lowercase()

        return when {
            brand.contains("samsung") || manufacturer.contains("samsung") || brand.contains("sec") -> OemDeviceType.SAMSUNG
            brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") || manufacturer.contains("xiaomi") -> OemDeviceType.XIAOMI_REDMI_POCO
            brand.contains("vivo") || brand.contains("iqoo") || manufacturer.contains("vivo") || manufacturer.contains("bbk") -> OemDeviceType.VIVO_IQOO
            brand.contains("oppo") || brand.contains("realme") || brand.contains("oneplus") || manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") || manufacturer.contains("oplus") -> OemDeviceType.REALME_OPPO_ONEPLUS
            brand.contains("google") || manufacturer.contains("google") || brand.contains("pixel") -> OemDeviceType.GOOGLE_PIXEL
            brand.contains("huawei") || brand.contains("honor") || manufacturer.contains("huawei") || manufacturer.contains("honor") -> OemDeviceType.HUAWEI_HONOR
            brand.contains("motorola") || brand.contains("lenovo") || brand.contains("moto") || manufacturer.contains("motorola") -> OemDeviceType.MOTOROLA
            brand.contains("infinix") || brand.contains("tecno") || brand.contains("itel") || manufacturer.contains("transsion") -> OemDeviceType.TRANSSION
            else -> OemDeviceType.GENERIC
        }
    }

    fun resolveEffectiveDevice(deviceTypeSetting: String?): OemDeviceType {
        if (deviceTypeSetting.isNullOrBlank() || deviceTypeSetting == OemDeviceType.AUTO.name) {
            return detectCurrentDevice()
        }
        return runCatching { OemDeviceType.valueOf(deviceTypeSetting) }.getOrDefault(detectCurrentDevice())
    }

    fun isScreenRecording(
        packageName: String,
        titleText: String,
        device: OemDeviceType = detectCurrentDevice()
    ): Boolean {
        val lowerText = titleText.lowercase()
        val isKeywordMatch = SCREEN_RECORDING_KEYWORDS.any { lowerText.contains(it) }

        val isPackageMatch = when (device) {
            OemDeviceType.SAMSUNG -> packageName in SAMSUNG_SCREEN_RECORDER_PACKAGES || packageName in ALL_OEM_RECORDER_PACKAGES
            OemDeviceType.XIAOMI_REDMI_POCO -> packageName in XIAOMI_SCREEN_RECORDER_PACKAGES || packageName in ALL_OEM_RECORDER_PACKAGES
            OemDeviceType.VIVO_IQOO -> packageName in VIVO_SCREEN_RECORDER_PACKAGES || packageName in ALL_OEM_RECORDER_PACKAGES
            OemDeviceType.REALME_OPPO_ONEPLUS -> packageName in REALME_OPPO_SCREEN_RECORDER_PACKAGES || packageName in ALL_OEM_RECORDER_PACKAGES
            OemDeviceType.HUAWEI_HONOR -> packageName in HUAWEI_SCREEN_RECORDER_PACKAGES || packageName in ALL_OEM_RECORDER_PACKAGES
            OemDeviceType.MOTOROLA -> packageName in MOTOROLA_SCREEN_RECORDER_PACKAGES || packageName in ALL_OEM_RECORDER_PACKAGES
            OemDeviceType.TRANSSION -> packageName in TRANSSION_SCREEN_RECORDER_PACKAGES || packageName in ALL_OEM_RECORDER_PACKAGES
            else -> packageName in ALL_OEM_RECORDER_PACKAGES
        }

        return isPackageMatch || isKeywordMatch
    }

    fun isScreenRecordingComplete(
        notification: Notification?,
        titleText: String
    ): Boolean {
        if (notification == null) return false
        val lowerText = titleText.lowercase()
        val isOngoing = (notification.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE)) != 0

        val completionKeywords = listOf(
            "saved", "complete", "completed", "finished", "stopped",
            "tap to view", "tap to share", "video saved", "recording saved",
            "audio saved", "ended", "tap to open", "tap to play"
        )
        if (completionKeywords.any { lowerText.contains(it) }) return true
        if (!isOngoing) return true

        return false
    }

    fun isInCallPackage(
        packageName: String,
        device: OemDeviceType = detectCurrentDevice()
    ): Boolean {
        return when (device) {
            OemDeviceType.SAMSUNG -> packageName in SAMSUNG_INCALL_PACKAGES || packageName in ALL_OEM_INCALL_PACKAGES
            OemDeviceType.XIAOMI_REDMI_POCO -> packageName in XIAOMI_INCALL_PACKAGES || packageName in ALL_OEM_INCALL_PACKAGES
            OemDeviceType.VIVO_IQOO -> packageName in VIVO_INCALL_PACKAGES || packageName in ALL_OEM_INCALL_PACKAGES
            OemDeviceType.REALME_OPPO_ONEPLUS -> packageName in REALME_OPPO_INCALL_PACKAGES || packageName in ALL_OEM_INCALL_PACKAGES
            OemDeviceType.HUAWEI_HONOR -> packageName in HUAWEI_INCALL_PACKAGES || packageName in ALL_OEM_INCALL_PACKAGES
            else -> packageName in ALL_OEM_INCALL_PACKAGES
        }
    }

    fun isHotspot(
        packageName: String,
        titleText: String
    ): Boolean {
        val lowerText = titleText.lowercase()
        return packageName in HOTSPOT_PACKAGES || HOTSPOT_KEYWORDS.any { lowerText.contains(it) }
    }

    fun isNonNavigationContent(
        packageName: String,
        titleText: String
    ): Boolean {
        val lowerText = titleText.lowercase()
        val isRecording = isScreenRecording(packageName, lowerText)
        val isHotspotAlert = isHotspot(packageName, lowerText)
        val isCallAlert = ALL_OEM_INCALL_PACKAGES.contains(packageName) || listOf("incoming call", "ongoing call", "calling...", "ringing").any { lowerText.contains(it) }
        return isRecording || isHotspotAlert || isCallAlert
    }

    fun getAutostartIntents(context: Context, device: OemDeviceType = detectCurrentDevice()): List<Intent> {
        val intents = mutableListOf<Intent>()

        when (device) {
            OemDeviceType.XIAOMI_REDMI_POCO -> {
                intents.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")))
                intents.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")))
            }
            OemDeviceType.REALME_OPPO_ONEPLUS -> {
                intents.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.oplus.securitypermission", "com.oplus.securitypermission.startup.StartupAppListActivity")))
            }
            OemDeviceType.VIVO_IQOO -> {
                intents.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")))
                intents.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")))
            }
            OemDeviceType.SAMSUNG -> {
                intents.add(Intent().setComponent(ComponentName("com.samsung.android.looper", "com.samsung.android.sm.ui.battery.BatteryActivity")))
                intents.add(Intent().setComponent(ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.ram.AutoRunActivity")))
                intents.add(Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.dashboard.SmartManagerDashBoardActivity")))
            }
            OemDeviceType.HUAWEI_HONOR -> {
                intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")))
                intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")))
            }
            OemDeviceType.MOTOROLA -> {
                intents.add(Intent().setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity")))
            }
            else -> {
                // Generic fallback
            }
        }

        // Generic fallback to App Info / Battery Settings
        try {
            intents.add(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )
        } catch (_: Throwable) { }

        return intents
    }
}
