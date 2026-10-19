/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import kotlin.math.abs

/**
 * Pure rules behind the battery Medium card (Rule_Book §4.12): the charging-speed label and the
 * time-until-empty estimate. Android has no public "time until empty", so it is derived from the
 * remaining charge and the current draw; anything implausible is reported as unknown so the card hides it.
 */
object BatteryEstimate {

    enum class Status(val label: String) {
        FastCharging("Fast Charging"),
        Charging("Charging"),
        Full("Fully Charged"),
        Discharging("Discharging")
    }

    /** Same threshold SystemUI uses for "Charging rapidly" (7.5 W). */
    const val FAST_CHARGING_MICRO_WATTS = 7_500_000L
    private const val DEFAULT_CHARGING_MICRO_VOLTS = 5_000_000L

    /** Some kernels report `CURRENT_NOW` in mA; a phone with its screen on never draws under 10 mA. */
    private const val MILLI_AMP_CUTOFF = 10_000L
    /** Likewise for `CHARGE_COUNTER` in mAh: no phone battery holds under 100 mAh. */
    private const val MILLI_AMP_HOUR_CUTOFF = 100_000L
    private const val MIN_ESTIMATE_MINUTES = 1L
    private const val MAX_ESTIMATE_MINUTES = 100L * 60L

    /** Charger power from the `max_charging_current` (µA) and `max_charging_voltage` (µV) battery extras. */
    fun chargingMicroWatts(maxMicroAmps: Int, maxMicroVolts: Int): Long {
        if (maxMicroAmps <= 0) return 0L
        val volts = if (maxMicroVolts > 0) maxMicroVolts.toLong() else DEFAULT_CHARGING_MICRO_VOLTS
        return (maxMicroAmps / 1000L) * (volts / 1000L)
    }

    fun status(charging: Boolean, full: Boolean, maxMicroAmps: Int, maxMicroVolts: Int): Status = when {
        full -> Status.Full
        !charging -> Status.Discharging
        chargingMicroWatts(maxMicroAmps, maxMicroVolts) > FAST_CHARGING_MICRO_WATTS -> Status.FastCharging
        else -> Status.Charging
    }

    /**
     * Minutes until empty from `BATTERY_PROPERTY_CHARGE_COUNTER` (µAh) and `BATTERY_PROPERTY_CURRENT_NOW` (µA,
     * negative on most devices while discharging), or null when the readings are missing or implausible.
     */
    fun minutesUntilEmpty(chargeCounterMicroAh: Long, currentNowMicroA: Long): Long? {
        if (chargeCounterMicroAh <= 0L || currentNowMicroA == 0L || currentNowMicroA == Long.MIN_VALUE) return null
        val charge = if (chargeCounterMicroAh < MILLI_AMP_HOUR_CUTOFF) chargeCounterMicroAh * 1000L else chargeCounterMicroAh
        val draw = abs(currentNowMicroA).let { if (it < MILLI_AMP_CUTOFF) it * 1000L else it }
        val minutes = charge * 60L / draw
        return minutes.takeIf { it in MIN_ESTIMATE_MINUTES..MAX_ESTIMATE_MINUTES }
    }

    /** "2 h 5 m" or "35 m". */
    fun formatMinutes(totalMinutes: Long): String {
        val h = totalMinutes / 60L
        val m = totalMinutes % 60L
        return if (h > 0) "$h h $m m" else "$m m"
    }
}
