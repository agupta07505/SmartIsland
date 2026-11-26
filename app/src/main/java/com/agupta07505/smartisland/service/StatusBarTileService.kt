/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.ShizukuManager
import com.agupta07505.smartisland.util.StatusBarReapplier
import com.agupta07505.smartisland.util.StatusBarSwitch
import com.agupta07505.smartisland.util.StatusBarTileLabel
import com.agupta07505.smartisland.util.StatusBarTileModel
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import com.agupta07505.smartisland.util.statusBarTileModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StatusBarTileService : TileService() {

    @Inject lateinit var repository: SmartIslandSettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var syncJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        syncJob?.cancel()
        syncJob = scope.launch {
            runSuspendCatchingLogged(TAG, "Tile sync failed") {
                val shizuku = flow {
                    while (true) {
                        emit(ShizukuManager.hasPermission())
                        delay(SHIZUKU_POLL_MS)
                    }
                }.distinctUntilChanged().flowOn(Dispatchers.Default)
                combine(
                    repository.settings.map { it.statusBarHidden }.distinctUntilChanged(),
                    StatusBarReapplier.shared,
                    shizuku
                ) { hidden, status, ready -> statusBarTileModel(hidden, ready, status) }
                    .distinctUntilChanged()
                    .collect { render(it) }
            }
        }
    }

    override fun onStopListening() {
        syncJob?.cancel()
        syncJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        if (isSecure && isLocked) unlockAndRun { toggle() } else toggle()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun toggle() {
        scope.launch {
            val hidden = repository.settings.first().statusBarHidden
            if (!hidden && !ShizukuManager.hasPermission()) {
                AppLogRecorder.w(TAG, "tile: Shizuku not ready, switching on ignored")
                Toast.makeText(
                    this@StatusBarTileService,
                    R.string.status_bar_tile_shizuku_required,
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            StatusBarSwitch.set(repository, !hidden, "tile")
        }
    }

    private fun render(model: StatusBarTileModel) {
        val tile = qsTile ?: return
        tile.state = if (model.active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.status_bar_tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(labelRes(model.label))
        }
        tile.updateTile()
    }

    private fun labelRes(label: StatusBarTileLabel): Int = when (label) {
        StatusBarTileLabel.Off -> R.string.status_bar_tile_off
        StatusBarTileLabel.Hidden -> R.string.status_bar_tile_hidden
        StatusBarTileLabel.Applying -> R.string.status_bar_tile_applying
        StatusBarTileLabel.Failed -> R.string.status_bar_tile_failed
        StatusBarTileLabel.NeedsShizuku -> R.string.status_bar_tile_needs_shizuku
    }

    private companion object {
        const val TAG = "StatusBarTile"
        const val SHIZUKU_POLL_MS = 1_500L
    }
}
