/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.di

import android.content.Context
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SmartIslandRepositoriesEntryPoint {
    fun settingsRepository(): SmartIslandSettingsRepository
    fun notificationRepository(): INotificationRepository
}

object SmartIslandRepositories {
    fun settingsRepository(context: Context): SmartIslandSettingsRepository =
        entryPoint(context).settingsRepository()

    fun notificationRepository(context: Context): INotificationRepository =
        entryPoint(context).notificationRepository()

    private fun entryPoint(context: Context): SmartIslandRepositoriesEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            SmartIslandRepositoriesEntryPoint::class.java
        )
}
