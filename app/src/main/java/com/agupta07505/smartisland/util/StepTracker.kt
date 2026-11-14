/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import java.util.Calendar

/**
 * Tracks daily steps via the device's hardware pedometer [Sensor.TYPE_STEP_COUNTER]
 * and system step providers (such as MIUI / HyperOS [miui.stepcounter.StepProvider]).
 * Zero CPU overhead: runs on hardware sensor hub DSP. Automatically detects midnight
 * day turnover and device reboots, keeping today's count accurate across reboots.
 */
internal object StepTracker {
    private const val PREFS = "idle_widget_steps"
    private const val KEY_DAY = "day"
    private const val KEY_BASELINE = "baseline"
    private const val KEY_OFFSET = "offset"
    private const val KEY_LAST_RAW = "last_raw"
    private const val KEY_SAVED_STEPS = "saved_steps"

    fun hasPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun hasSensor(context: Context): Boolean {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return false
        return sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
    }

    fun todayKey(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
    }

    /** Returns today's step count using the most accurate available source. */
    fun getTodaySteps(context: Context): Int {
        if (!hasPermission(context)) return -1
        // 1. First, check system step provider (MIUI / HyperOS Steps.db)
        val providerSteps = readMiuiSteps(context)
        if (providerSteps >= 0) {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            p.edit()
                .putInt(KEY_DAY, todayKey())
                .putInt(KEY_SAVED_STEPS, providerSteps)
                .apply()
            return providerSteps
        }
        // 2. Hardware pedometer cached steps
        return cachedSteps(context)
    }

    /** Returns the cached steps for today (or -1 if never initialized or no permission). */
    fun cachedSteps(context: Context): Int {
        if (!hasPermission(context)) return -1
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val savedDay = p.getInt(KEY_DAY, -1)
        if (savedDay == todayKey()) {
            val steps = p.getInt(KEY_SAVED_STEPS, 0)
            if (steps > 0) return steps
        }
        val providerSteps = readMiuiSteps(context)
        if (providerSteps >= 0) {
            p.edit()
                .putInt(KEY_DAY, todayKey())
                .putInt(KEY_SAVED_STEPS, providerSteps)
                .apply()
            return providerSteps
        }
        return if (savedDay == todayKey()) p.getInt(KEY_SAVED_STEPS, 0) else 0
    }

    /**
     * Reads today's total steps directly from Xiaomi / POCO / Redmi HyperOS StepProvider.
     * Returns -1 if unavailable or not on MIUI.
     */
    fun readMiuiSteps(context: Context): Int {
        return runCatching {
            val uri = Uri.parse("content://com.miui.providers.steps/item")
            val midnight = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val cursor = context.contentResolver.query(
                uri,
                arrayOf("_begin_time", "_steps"),
                "_begin_time >= ?",
                arrayOf(midnight.toString()),
                null
            ) ?: return -1

            var total = 0
            cursor.use { c ->
                val stepsIdx = c.getColumnIndex("_steps")
                if (stepsIdx >= 0) {
                    while (c.moveToNext()) {
                        total += c.getInt(stepsIdx)
                    }
                }
            }
            total
        }.getOrDefault(-1)
    }

    /**
     * Registers a listener for [Sensor.TYPE_STEP_COUNTER]. Returns a cleanup lambda to unregister.
     * When new sensor values arrive, [onStepCount] is invoked with the calculated steps for today.
     */
    fun register(context: Context, onStepCount: (Int) -> Unit): (() -> Unit)? {
        if (!hasPermission(context)) return null
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return null
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null

        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || event.values.isEmpty()) return
                val raw = event.values[0].toInt()
                val today = todayKey()
                val savedDay = p.getInt(KEY_DAY, -1)
                var baseline = p.getInt(KEY_BASELINE, -1)
                var offset = p.getInt(KEY_OFFSET, 0)
                val lastRaw = p.getInt(KEY_LAST_RAW, -1)

                val midnight = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val bootTime = System.currentTimeMillis() - SystemClock.elapsedRealtime()

                val todaySteps: Int
                if (savedDay != today || baseline < 0) {
                    baseline = if (bootTime >= midnight) {
                        // Device booted today after midnight: all raw steps were walked today!
                        0
                    } else if (lastRaw in 0..raw) {
                        lastRaw
                    } else {
                        val miui = readMiuiSteps(context)
                        if (miui in 0..raw) raw - miui else raw
                    }
                    offset = 0
                    todaySteps = (raw - baseline).coerceAtLeast(0)
                    p.edit()
                        .putInt(KEY_DAY, today)
                        .putInt(KEY_BASELINE, baseline)
                        .putInt(KEY_OFFSET, offset)
                        .putInt(KEY_LAST_RAW, raw)
                        .putInt(KEY_SAVED_STEPS, todaySteps)
                        .apply()
                } else {
                    if (raw < lastRaw) {
                        // Device rebooted mid-day: raw reset to 0! Add previous steps to offset!
                        offset += (lastRaw - baseline).coerceAtLeast(0)
                        baseline = 0
                    }
                    todaySteps = (offset + (raw - baseline)).coerceAtLeast(0)
                    p.edit()
                        .putInt(KEY_BASELINE, baseline)
                        .putInt(KEY_OFFSET, offset)
                        .putInt(KEY_LAST_RAW, raw)
                        .putInt(KEY_SAVED_STEPS, todaySteps)
                        .apply()
                }
                onStepCount(todaySteps)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val registered = sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        if (!registered) return null

        return {
            runCatching { sm.unregisterListener(listener) }
        }
    }
}
