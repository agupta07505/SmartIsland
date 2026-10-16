/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartIslandApp : Application() {
    override fun onCreate() {
        super.onCreate()
        bypassHiddenApis()
    }

    private fun bypassHiddenApis() {
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
            setHiddenApiExemptions.invoke(
                vmRuntime,
                arrayOf(
                    "Landroid/view/ViewTreeObserver",
                    "Landroid/bluetooth/BluetoothDevice;->getBatteryLevel",
                    "Landroid/media/session/"
                ) as Any
            )
            android.util.Log.d("SmartIslandApp", "Hidden API exemptions applied for overlay insets, Bluetooth battery and media session")
        } catch (e: Exception) {
            android.util.Log.e("SmartIslandApp", "Failed to bypass Hidden API restrictions", e)
        }
    }
}
