/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland

import android.app.Application
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartIslandApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // A process the system restarted keeps recording from its first line, before settings load.
        com.agupta07505.smartisland.util.AppLogRecorder.restore(this)
        installCrashLogger()
        bypassHiddenApis()
    }

    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { com.agupta07505.smartisland.util.AppLogRecorder.recordCrash(thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun bypassHiddenApis() {
        // Hidden API restrictions only exist on API 28+, and HiddenApiBypass requires API 28.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return

        // Maintained library first; the hand-written reflection below stays as a fallback.
        val exempted = runCatching {
            org.lsposed.hiddenapibypass.HiddenApiBypass.addHiddenApiExemptions("L")
        }.getOrDefault(false)
        if (exempted) {
            android.util.Log.d("SmartIslandApp", "Hidden API restrictions exempted (HiddenApiBypass)")
            return
        }
        try {
            val forName = Class::class.java.getDeclaredMethod("forName", String::class.java)
            val getDeclaredMethod = Class::class.java.getDeclaredMethod(
                "getDeclaredMethod",
                String::class.java,
                arrayOf<Class<*>>().javaClass
            )
            
            val vmRuntimeClass = forName.invoke(null, "dalvik.system.VMRuntime") as Class<*>
            val getRuntime = getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null) as java.lang.reflect.Method
            val setHiddenApiExemptions = getDeclaredMethod.invoke(
                vmRuntimeClass,
                "setHiddenApiExemptions",
                arrayOf(arrayOf<String>().javaClass)
            ) as java.lang.reflect.Method
            
            val vmRuntime = getRuntime.invoke(null)
            setHiddenApiExemptions.invoke(vmRuntime, arrayOf("L") as Any)
            android.util.Log.d("SmartIslandApp", "Successfully bypassed Hidden API restrictions (unsealed reflection)")
        } catch (e: Exception) {
            android.util.Log.e("SmartIslandApp", "Failed to bypass Hidden API restrictions", e)
        }
    }
}
