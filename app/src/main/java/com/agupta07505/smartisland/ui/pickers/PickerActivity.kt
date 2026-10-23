/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.pickers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.UtilityLayout
import com.agupta07505.smartisland.ui.SmartIslandTheme
import com.agupta07505.smartisland.util.UtilityWidgets
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * The window the island opens for its pickers (the pinned apps picker and the "add a utility" picker): a see-through
 * activity in its own task that holds only the dialog, so choosing never brings the app's settings screen up.
 */
@AndroidEntryPoint
class PickerActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SmartIslandSettingsRepository

    private var kind by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kind = PickerKinds.resolve(intent?.getStringExtra(EXTRA_KIND))
        if (kind == null) { finish(); return }

        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = SmartIslandSettings.Default
            )
            SmartIslandTheme(
                themeMode = settings.themeMode,
                liquidGlass = settings.liquidGlassTheme,
                materialYou = settings.materialYou,
                accentColor = settings.accentColor
            ) {
                when (kind) {
                    KIND_APPS -> AppPickerDialog(
                        settings = settings,
                        repository = settingsRepository,
                        onDismiss = { finish() }
                    )
                    KIND_UTILITY -> UtilityPickerHost(
                        settings = settings,
                        repository = settingsRepository,
                        onClose = { finish() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val requested = PickerKinds.resolve(intent.getStringExtra(EXTRA_KIND))
        if (requested != null) kind = requested else finish()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // An app widget's setup screen reports back by request code (see UtilityWidgets.configure); passed on as usual too.
        UtilityWidgets.onActivityResult(requestCode, resultCode)
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun finish() {
        super.finish()
        // The window is see-through and short-lived; no transition when it goes.
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val EXTRA_KIND = "picker_kind"
        const val KIND_APPS = PickerKinds.APPS
        const val KIND_UTILITY = PickerKinds.UTILITY

        fun intentFor(context: Context, kind: String): Intent =
            Intent(context.applicationContext, PickerActivity::class.java)
                .putExtra(EXTRA_KIND, kind)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
    }
}

/**
 * The "add a utility" picker in its window: the add dialog opens on the first empty cell of the Utilities grid (the
 * same cell the settings editor picks).
 */
@Composable
internal fun UtilityPickerHost(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    // The settings flow starts with the defaults; the cell must come from the saved layout, so wait for the first real value.
    val loaded by produceState<SmartIslandSettings?>(null, repository) { value = repository.settings.first() }
    val firstLayout = loaded?.utilityLayout ?: return
    val cell = remember { firstEmptyUtilityCell(UtilityLayout.decode(firstLayout)) }
    if (cell == null) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, R.string.utilities_editor_full, Toast.LENGTH_SHORT).show()
            onClose()
        }
        return
    }
    UtilityAddFlow(settings = settings, repository = repository, row = cell.first, col = cell.second, onClose = onClose)
}
