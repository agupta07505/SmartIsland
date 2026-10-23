/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.CityResult
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.model.WeatherConfig
import com.agupta07505.smartisland.model.WeatherPill
import com.agupta07505.smartisland.model.WeatherReadouts
import com.agupta07505.smartisland.model.WeatherRules
import com.agupta07505.smartisland.ui.glass.Card
import com.agupta07505.smartisland.util.IdleWeather
import kotlinx.coroutines.launch

private enum class SearchState { Idle, Searching, NoResults }

private enum class RefreshState { Idle, Working, Done, Failed }

/**
 * Settings > Idle widgets > Weather: where the place comes from (device location, a searched city, or the last known
 * place), how often the weather is asked for, and which service answers.
 */
@Composable
internal fun WeatherSettingsCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = settings.weatherPrefs()
    var locationGranted by remember { mutableStateOf(IdleWeather.hasLocationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        locationGranted = granted
        // The app is in front now: this is the moment the last known place can be read and kept.
        if (granted) scope.launch { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { IdleWeather.captureLocation(context) } }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = stringResource(R.string.weather_cfg_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Note(stringResource(R.string.weather_cfg_intro))
            if (!settings.allowNetworkChecks) Note(stringResource(R.string.idle_widgets_weather_needs_network))

            Choices(
                title = stringResource(R.string.weather_cfg_source_title),
                options = listOf(
                    WeatherConfig.SOURCE_AUTO to stringResource(R.string.weather_cfg_source_auto),
                    WeatherConfig.SOURCE_MANUAL to stringResource(R.string.weather_cfg_source_manual),
                    WeatherConfig.SOURCE_LAST_KNOWN to stringResource(R.string.weather_cfg_source_last)
                ),
                selected = prefs.source,
                onSelect = { scope.launch { repository.setWeatherLocationSource(it) } }
            )
            when (prefs.source) {
                WeatherConfig.SOURCE_MANUAL -> ManualCity(prefs.manualCity, repository)
                WeatherConfig.SOURCE_LAST_KNOWN -> {
                    Note(stringResource(R.string.weather_cfg_source_last_desc))
                    if (!locationGranted) {
                        Note(stringResource(R.string.weather_cfg_last_needs_permission))
                        PermissionButton { permissionLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION) }
                    } else if (!IdleWeather.hasStoredPlace(context)) {
                        Note(stringResource(R.string.weather_cfg_last_none))
                    }
                }
                else -> {
                    Note(stringResource(R.string.weather_cfg_source_auto_desc))
                    if (!locationGranted) PermissionButton { permissionLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION) }
                }
            }

            Choices(
                title = stringResource(R.string.weather_cfg_pill_title),
                options = listOf(
                    WeatherPill.TEMP_ICON to stringResource(R.string.weather_cfg_pill_temp_icon),
                    WeatherPill.TEMP_ONLY to stringResource(R.string.weather_cfg_pill_temp_only),
                    WeatherPill.FEELS to stringResource(R.string.weather_cfg_pill_feels),
                    WeatherPill.TEXT to stringResource(R.string.weather_cfg_pill_text)
                ),
                selected = WeatherPill.normalize(settings.weatherPillMode),
                onSelect = { scope.launch { repository.setWeatherPillMode(it) } }
            )
            Note(stringResource(R.string.weather_cfg_pill_desc))

            MultiChoices(
                title = stringResource(R.string.weather_cfg_readouts_title),
                options = listOf(
                    WeatherReadouts.HIGH_LOW to stringResource(R.string.weather_cfg_readout_high_low),
                    WeatherReadouts.RAIN to stringResource(R.string.weather_cfg_readout_rain),
                    WeatherReadouts.HUMIDITY to stringResource(R.string.weather_cfg_readout_humidity),
                    WeatherReadouts.UV to stringResource(R.string.weather_cfg_readout_uv),
                    WeatherReadouts.SUN to stringResource(R.string.weather_cfg_readout_sun),
                    WeatherReadouts.AQI to stringResource(R.string.weather_cfg_readout_aqi),
                    WeatherReadouts.HOURLY to stringResource(R.string.weather_cfg_readout_hourly)
                ),
                selected = WeatherReadouts.parse(settings.weatherIslandReadouts).toSet(),
                onToggle = { scope.launch { repository.setWeatherIslandReadouts(WeatherReadouts.toggle(settings.weatherIslandReadouts, it)) } }
            )
            Note(stringResource(R.string.weather_cfg_readouts_desc))
            if (prefs.provider == WeatherConfig.PROVIDER_CUSTOM && WeatherReadouts.AQI in WeatherReadouts.parse(settings.weatherIslandReadouts)) {
                Note(stringResource(R.string.weather_cfg_readout_aqi_custom))
            }

            SwitchRow(
                title = stringResource(R.string.weather_cfg_icon_animated),
                checked = settings.weatherIconAnimated,
                onCheckedChange = { scope.launch { repository.setWeatherIconAnimated(it) } }
            )
            if (settings.weatherIconAnimated) {
                SwitchRow(
                    title = stringResource(R.string.weather_cfg_icon_saver),
                    checked = settings.weatherIconPauseOnSaver,
                    onCheckedChange = { scope.launch { repository.setWeatherIconPauseOnSaver(it) } }
                )
            }
            Note(stringResource(R.string.weather_cfg_icon_desc))

            SmartDisplayRules(settings, repository)

            Choices(
                title = stringResource(R.string.weather_cfg_refresh_title),
                options = WeatherConfig.REFRESH_CHOICES.map { it to refreshLabel(it) },
                selected = prefs.refreshMinutes,
                onSelect = { scope.launch { repository.setWeatherRefreshMinutes(it) } }
            )
            Note(
                stringResource(
                    if (prefs.refreshMinutes == WeatherConfig.REFRESH_MANUAL) R.string.weather_cfg_refresh_manual_desc
                    else R.string.weather_cfg_refresh_desc
                )
            )
            RefreshNow(enabled = settings.allowNetworkChecks, prefs = prefs)

            Choices(
                title = stringResource(R.string.weather_cfg_provider_title),
                options = listOf(
                    WeatherConfig.PROVIDER_OPEN_METEO to stringResource(R.string.weather_cfg_provider_open_meteo),
                    WeatherConfig.PROVIDER_CUSTOM to stringResource(R.string.weather_cfg_provider_custom)
                ),
                selected = prefs.provider,
                onSelect = { scope.launch { repository.setWeatherProvider(it) } }
            )
            if (prefs.provider == WeatherConfig.PROVIDER_CUSTOM) {
                CustomProvider(savedUrl = prefs.customUrl, savedKey = prefs.customKey, repository = repository)
            } else {
                Note(stringResource(R.string.weather_cfg_provider_open_meteo_desc))
            }
        }
    }
}

@Composable
private fun SmartDisplayRules(settings: SmartIslandSettings, repository: SmartIslandSettingsRepository) {
    val scope = rememberCoroutineScope()
    val rules = settings.weatherRules()
    val triggers = WeatherRules.parseTriggers(rules.triggers)
    val fahrenheit = IdleWidgets.useFahrenheit(settings.idleWidgetTempUnit, java.util.Locale.getDefault().country)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Choices(
            title = stringResource(R.string.weather_cfg_rule_title),
            options = listOf(
                WeatherRules.MODE_ALWAYS to stringResource(R.string.weather_cfg_rule_always),
                WeatherRules.MODE_NOTABLE to stringResource(R.string.weather_cfg_rule_notable)
            ),
            selected = rules.mode,
            onSelect = { scope.launch { repository.setWeatherShowRule(it) } }
        )
        if (rules.mode == WeatherRules.MODE_NOTABLE) {
            MultiChoices(
                title = stringResource(R.string.weather_cfg_trigger_title),
                options = listOf(
                    WeatherRules.TRIGGER_RAIN to stringResource(R.string.weather_cfg_trigger_rain),
                    WeatherRules.TRIGGER_HOT to stringResource(R.string.weather_cfg_trigger_hot),
                    WeatherRules.TRIGGER_COLD to stringResource(R.string.weather_cfg_trigger_cold),
                    WeatherRules.TRIGGER_SEVERE to stringResource(R.string.weather_cfg_trigger_severe)
                ),
                selected = triggers.toSet(),
                onToggle = { scope.launch { repository.setWeatherNotableTriggers(WeatherRules.toggleTrigger(rules.triggers, it)) } }
            )
            if (WeatherRules.TRIGGER_HOT in triggers) {
                Stepper(
                    text = stringResource(R.string.weather_cfg_hot_at, IdleWidgets.displayTemp(rules.hotC.toFloat(), fahrenheit)),
                    canDecrease = rules.hotC > WeatherRules.MIN_HOT_C,
                    canIncrease = rules.hotC < WeatherRules.MAX_HOT_C,
                    onChange = { scope.launch { repository.setWeatherHotC(rules.hotC + it) } }
                )
            }
            if (WeatherRules.TRIGGER_COLD in triggers) {
                Stepper(
                    text = stringResource(R.string.weather_cfg_cold_at, IdleWidgets.displayTemp(rules.coldC.toFloat(), fahrenheit)),
                    canDecrease = rules.coldC > WeatherRules.MIN_COLD_C,
                    canIncrease = rules.coldC < WeatherRules.MAX_COLD_C,
                    onChange = { scope.launch { repository.setWeatherColdC(rules.coldC + it) } }
                )
            }
            Note(stringResource(R.string.weather_cfg_trigger_desc))
        }
        SwitchRow(
            title = stringResource(R.string.weather_cfg_night_title),
            checked = rules.hideAtNight,
            onCheckedChange = { scope.launch { repository.setWeatherHideAtNight(it) } }
        )
        if (rules.hideAtNight) {
            Stepper(
                text = stringResource(R.string.weather_cfg_night_from, hourText(rules.nightStartHour)),
                canDecrease = true,
                canIncrease = true,
                onChange = { scope.launch { repository.setWeatherNightStartHour(Math.floorMod(rules.nightStartHour + it, 24)) } }
            )
            Stepper(
                text = stringResource(R.string.weather_cfg_night_to, hourText(rules.nightEndHour)),
                canDecrease = true,
                canIncrease = true,
                onChange = { scope.launch { repository.setWeatherNightEndHour(Math.floorMod(rules.nightEndHour + it, 24)) } }
            )
        }
        if (rules.active) Note(stringResource(R.string.weather_cfg_rules_desc))
    }
}

private fun hourText(hour: Int): String = String.format(java.util.Locale.US, "%02d:00", hour)

@Composable
private fun Stepper(text: String, canDecrease: Boolean, canIncrease: Boolean, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        OutlinedButton(enabled = canDecrease, onClick = { onChange(-1) }) { Text(stringResource(R.string.weather_cfg_step_minus)) }
        OutlinedButton(enabled = canIncrease, onClick = { onChange(1) }) { Text(stringResource(R.string.weather_cfg_step_plus)) }
    }
}

@Composable
private fun refreshLabel(minutes: Int): String = when (minutes) {
    WeatherConfig.REFRESH_MANUAL -> stringResource(R.string.weather_cfg_refresh_manual)
    15 -> stringResource(R.string.weather_cfg_refresh_15m)
    30 -> stringResource(R.string.weather_cfg_refresh_30m)
    60 -> stringResource(R.string.weather_cfg_refresh_1h)
    else -> stringResource(R.string.weather_cfg_refresh_3h)
}

@Composable
private fun PermissionButton(onClick: () -> Unit) {
    Button(onClick = onClick) { Text(stringResource(R.string.idle_widgets_allow_location)) }
}

@Composable
private fun ManualCity(chosen: String, repository: SmartIslandSettingsRepository) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CityResult>>(emptyList()) }
    var state by remember { mutableStateOf(SearchState.Idle) }

    fun search() {
        if (query.trim().length < WeatherConfig.MIN_SEARCH_LENGTH || state == SearchState.Searching) return
        state = SearchState.Searching
        scope.launch {
            val found = IdleWeather.searchCities(query)
            results = found
            state = if (found.isEmpty()) SearchState.NoResults else SearchState.Idle
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (chosen.isBlank()) stringResource(R.string.weather_cfg_city_none) else stringResource(R.string.weather_cfg_city_chosen, chosen),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(WeatherConfig.MAX_CITY_LENGTH) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(R.string.weather_cfg_city_search_hint)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() })
            )
            Button(onClick = { search() }, enabled = query.trim().length >= WeatherConfig.MIN_SEARCH_LENGTH && state != SearchState.Searching) {
                Text(stringResource(R.string.weather_cfg_city_search))
            }
        }
        when (state) {
            SearchState.Searching -> Note(stringResource(R.string.weather_cfg_city_searching))
            SearchState.NoResults -> Note(stringResource(R.string.weather_cfg_city_no_results))
            SearchState.Idle -> Unit
        }
        results.forEach { city ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable {
                        scope.launch { repository.setWeatherManualCity(city.label, city.lat, city.lon) }
                        results = emptyList()
                        query = ""
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = city.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Note(stringResource(R.string.weather_cfg_city_privacy))
    }
}

@Composable
private fun RefreshNow(enabled: Boolean, prefs: com.agupta07505.smartisland.model.WeatherPrefs) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var state by remember { mutableStateOf(RefreshState.Idle) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            enabled = enabled && state != RefreshState.Working,
            onClick = {
                state = RefreshState.Working
                scope.launch {
                    val fresh = IdleWeather.fetch(context, prefs)
                    if (fresh != null) IdleWeather.notifyRefreshed()
                    state = if (fresh != null) RefreshState.Done else RefreshState.Failed
                }
            }
        ) { Text(stringResource(R.string.weather_cfg_refresh_now)) }
        val status = when (state) {
            RefreshState.Working -> R.string.weather_cfg_refresh_working
            RefreshState.Done -> R.string.weather_cfg_refresh_done
            RefreshState.Failed -> R.string.weather_cfg_refresh_failed
            RefreshState.Idle -> null
        }
        if (status != null) Note(stringResource(status))
    }
}

@Composable
private fun CustomProvider(savedUrl: String, savedKey: String, repository: SmartIslandSettingsRepository) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(savedUrl) }
    var key by remember { mutableStateOf(savedKey) }
    val urlOk = WeatherConfig.isValidCustomUrl(url)
    val changed = WeatherConfig.normalizeCustomUrl(url) != savedUrl || WeatherConfig.normalizeKey(key) != savedKey
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it.take(WeatherConfig.MAX_URL_LENGTH) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = url.isNotBlank() && !urlOk,
            label = { Text(stringResource(R.string.weather_cfg_custom_url)) },
            placeholder = { Text("https://example.com/v1/forecast") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next)
        )
        if (url.isNotBlank() && !urlOk) Note(stringResource(R.string.weather_cfg_custom_url_invalid))
        OutlinedTextField(
            value = key,
            onValueChange = { key = it.take(WeatherConfig.MAX_KEY_LENGTH) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.weather_cfg_custom_key)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
        )
        Button(
            enabled = changed && (url.isBlank() || urlOk),
            onClick = {
                scope.launch {
                    repository.setWeatherCustomUrl(url)
                    repository.setWeatherCustomKey(key)
                }
            }
        ) { Text(stringResource(R.string.weather_cfg_custom_save)) }
        Note(stringResource(R.string.weather_cfg_custom_desc))
    }
}

@Composable
private fun Note(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MultiChoices(title: String, options: List<Pair<String, String>>, selected: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        options.chunked(3).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                rowOptions.forEach { (id, label) ->
                    val on = id in selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onToggle(id) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                            color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
                repeat(3 - rowOptions.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun <T> Choices(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            options.forEach { (id, label) ->
                val on = selected == id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onSelect(id) }
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
