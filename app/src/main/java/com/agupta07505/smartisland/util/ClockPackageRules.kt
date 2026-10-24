/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/** Kept separate so OEM notification workarounds stay explicit. */
internal object ClockPackageRules {
    private val OEM_CLOCK_PACKAGES = setOf(
        "com.sec.android.app.clockpackage",
        "com.miui.deskclock",
        "com.oneplus.deskclock",
        "com.coloros.alarmclock",
        "com.oplus.alarmclock",
        "com.android.bbkclock",
        "com.vivo.clock",
        "com.huawei.deskclock",
        "com.hihonor.deskclock",
        "com.motorola.blur.alarmclock",
        "com.motorola.timeweatherwidget",
        "com.sec.android.deskclock",
        "com.asus.deskclock",
        "com.lenovo.deskclock",
        "com.sonyericsson.organizer",
        "com.htc.android.worldclock"
    )

    private val CLOCK_PACKAGES = OEM_CLOCK_PACKAGES + setOf(
        "com.google.android.deskclock",
        "com.android.deskclock",
        "com.timer.stopwatch",
        "com.hybrid.stopwatch",
        "com.sportstracklive.stopwatch",
        "com.jee.timer",
        "com.clover.timer"
    )

    private val OEM_PACKAGE_PREFIXES = listOf(
        "com.miui.", "com.xiaomi.", "com.mi.", "com.sec.", "com.samsung.", "com.oneplus.", "com.oplus.",
        "com.oppo.", "com.coloros.", "com.heytap.", "com.realme.", "com.vivo.", "com.bbk.", "com.huawei.",
        "com.hihonor.", "com.honor.", "com.asus.", "com.lenovo.", "com.motorola.", "com.sonyericsson.",
        "com.sonymobile.", "com.htc.", "com.lge.", "com.zte.", "com.nubia.", "com.meizu.", "com.nothing.",
        "com.transsion.", "com.tecno.", "com.infinix.", "com.itel."
    )

    fun isKnownClockPackage(packageName: String): Boolean = packageName in CLOCK_PACKAGES

    fun isOemClock(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return pkg in OEM_CLOCK_PACKAGES || OEM_PACKAGE_PREFIXES.any { pkg.startsWith(it) }
    }

    fun usesShadeFixes(packageName: String): Boolean = !isOemClock(packageName)
}
