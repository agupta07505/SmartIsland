/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/** One vendor settings screen, by component. [passPackage] adds this app's package name as an extra. */
data class OemTarget(
    val packageName: String,
    val className: String,
    val passPackage: Boolean = false
)

/**
 * Rule_Book 7.2's `OEMIntentFactory`: the hidden vendor panels for "Auto-start" and background running on
 * the three skins that kill overlays hardest. Pure data (no Android types) so the choice is unit-testable;
 * [OemAutostartUtil.openBackgroundLockSettings] turns it into intents and falls back to App info.
 * Candidates are in order of preference: the first one that opens wins.
 */
object OemIntentFactory {

    /** Skins where onboarding shows the "Lock in background / Auto-start" step. */
    fun needsBackgroundLockStep(device: OemDeviceType): Boolean = device in BACKGROUND_LOCK_DEVICES

    fun backgroundTargets(device: OemDeviceType): List<OemTarget> = when (device) {
        // MIUI / HyperOS: Autostart, then the per-app battery saver ("No restrictions").
        OemDeviceType.XIAOMI_REDMI_POCO -> listOf(
            OemTarget("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            OemTarget("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity", passPackage = true)
        )
        // ColorOS / Realme UI / OxygenOS: Startup manager, OnePlus chain launch, then power usage.
        OemDeviceType.REALME_OPPO_ONEPLUS -> listOf(
            OemTarget("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            OemTarget("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            OemTarget("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"),
            OemTarget("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
            OemTarget("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity")
        )
        // FuntouchOS / OriginOS: background start-up manager, then high background power use.
        OemDeviceType.VIVO_IQOO -> listOf(
            OemTarget("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            OemTarget("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
            OemTarget("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
            OemTarget("com.vivo.abe", "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity")
        )
        else -> emptyList()
    }

    /** Extra keys MIUI's power keeper reads to open on one app. */
    const val EXTRA_PACKAGE_NAME = "package_name"
    const val EXTRA_PACKAGE_LABEL = "package_label"

    private val BACKGROUND_LOCK_DEVICES = setOf(
        OemDeviceType.XIAOMI_REDMI_POCO,
        OemDeviceType.REALME_OPPO_ONEPLUS,
        OemDeviceType.VIVO_IQOO
    )
}
