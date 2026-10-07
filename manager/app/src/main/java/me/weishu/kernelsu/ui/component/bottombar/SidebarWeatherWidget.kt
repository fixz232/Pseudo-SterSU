package me.weishu.kernelsu.ui.component.bottombar

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.util.SidebarWeatherCondition
import me.weishu.kernelsu.ui.util.SidebarWeatherError
import me.weishu.kernelsu.ui.util.SidebarWeatherRuntime
import me.weishu.kernelsu.ui.util.SidebarWidgetConfig
import me.weishu.kernelsu.ui.util.rememberSidebarWeatherState
import me.weishu.kernelsu.ui.util.weatherCondition
import java.util.Date

@Composable
internal fun SidebarWeatherSummary(config: SidebarWidgetConfig, compact: Boolean) {
    val state = rememberSidebarWeatherState(config.weatherApi)
    val temperature = if (config.weatherApi.enabled) {
        state.reading?.temperatureText()?.let { it + if (config.weatherApi.fahrenheit) "°F" else "°C" }.orEmpty()
    } else config.weatherTemperature
    val description = if (config.weatherApi.enabled) state.reading?.description.orEmpty() else config.weatherLabel
    val text = listOf(description, temperature).filter(String::isNotBlank).joinToString(" ").ifBlank {
        if (state.loading) stringResource(R.string.sidebar_weather_updating) else "—"
    }
    Text(
        text = text,
        fontSize = if (compact) 10.sp else 12.sp,
        lineHeight = if (compact) 14.sp else 16.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun SidebarWeatherWidget(config: SidebarWidgetConfig, compact: Boolean) {
    val state = rememberSidebarWeatherState(config.weatherApi)
    val api = config.weatherApi.enabled
    val reading = state.reading
    val ink = LocalContentColor.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDetails by remember(config.weatherApi) { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    val temperature = if (api) reading?.temperatureText()?.plus("\u00b0") ?: "\u2014"
        else config.weatherTemperature.ifBlank { "\u2014" }
    val description = if (api) reading?.description.orEmpty() else config.weatherLabel
    val condition = if (api) reading?.condition ?: SidebarWeatherCondition.Unknown else weatherCondition(description)
    val location = if (api) reading?.location.orEmpty().ifBlank { config.weatherApi.locationLabel } else ""
    val status = stringResource(when {
        !api -> R.string.sidebar_weather_manual
        state.loading -> R.string.sidebar_weather_updating
        state.error != null && reading != null -> R.string.sidebar_weather_cached
        state.error != null -> R.string.sidebar_weather_unavailable
        reading == null -> R.string.sidebar_weather_waiting
        else -> R.string.sidebar_weather_updated
    })
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(SidebarPaneShape)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.sidebar_weather_details)) { showDetails = true }
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 2.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier.size(if (compact) 40.dp else 48.dp)
                .background(ink.copy(alpha = 0.07f), SidebarPaneShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(condition.icon(), contentDescription = null, modifier = Modifier.size(if (compact) 26.dp else 32.dp))
        }
        Text(
            temperature,
            fontSize = when { temperature.length > 4 -> 18.sp; compact -> 26.sp; else -> 30.sp },
            lineHeight = if (compact) 32.sp else 36.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            description.ifBlank { stringResource(R.string.sidebar_weather_no_description) },
            fontSize = if (compact) 11.sp else 12.sp,
            lineHeight = if (compact) 14.sp else 16.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (location.isNotBlank()) Text(
            location, fontSize = 11.sp, lineHeight = 14.sp, color = ink.copy(alpha = 0.8f),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            status, fontSize = 10.sp, lineHeight = 12.sp, color = ink.copy(alpha = 0.8f),
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }

    if (showDetails) AlertDialog(
        onDismissRequest = { showDetails = false },
        title = { Text(location.ifBlank { stringResource(R.string.sidebar_weather_details) }) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(condition.icon(), contentDescription = null, modifier = Modifier.size(48.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (api && reading != null) temperature + if (config.weatherApi.fahrenheit) "F" else "C" else temperature,
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text(description.ifBlank { stringResource(R.string.sidebar_weather_no_description) })
                    }
                }
                if (state.loading || refreshing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (api && reading != null) {
                    val date = Date(reading.fetchedAt)
                    val updated = DateFormat.getMediumDateFormat(context).format(date) + " " + DateFormat.getTimeFormat(context).format(date)
                    Text(stringResource(R.string.sidebar_weather_updated_at, updated), style = MaterialTheme.typography.bodySmall)
                }
                if (api && state.error != null) Text(
                    weatherErrorText(state.error, state.httpStatus),
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    stringResource(if (api) R.string.sidebar_weather_refresh_help else R.string.sidebar_weather_manual_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { showDetails = false }) { Text(stringResource(R.string.sidebar_weather_close)) }
        },
        dismissButton = {
            if (api) TextButton(
                enabled = !state.loading && !refreshing,
                onClick = {
                    refreshing = true
                    scope.launch {
                        try { SidebarWeatherRuntime.repository(context).refresh(config.weatherApi, force = true) }
                        finally { refreshing = false }
                    }
                },
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.sidebar_weather_refresh), modifier = Modifier.padding(start = 6.dp))
            }
        },
    )
}

internal fun SidebarWeatherCondition.icon(): ImageVector = when (this) {
    SidebarWeatherCondition.Clear -> Icons.Rounded.WbSunny
    SidebarWeatherCondition.Cloud -> Icons.Rounded.Cloud
    SidebarWeatherCondition.Rain -> Icons.Rounded.Grain
    SidebarWeatherCondition.Snow -> Icons.Rounded.AcUnit
    SidebarWeatherCondition.Storm -> Icons.Rounded.Thunderstorm
    SidebarWeatherCondition.Fog -> Icons.Rounded.Air
    SidebarWeatherCondition.Night -> Icons.Rounded.NightsStay
    SidebarWeatherCondition.Unknown -> Icons.Rounded.Thermostat
}

@Composable
internal fun weatherErrorText(error: SidebarWeatherError, httpStatus: Int? = null): String = when (error) {
    SidebarWeatherError.Http -> stringResource(R.string.sidebar_weather_error_http, httpStatus ?: 0)
    else -> stringResource(when (error) {
        SidebarWeatherError.Url -> R.string.sidebar_weather_error_url
        SidebarWeatherError.Header -> R.string.sidebar_weather_error_header
        SidebarWeatherError.Path -> R.string.sidebar_weather_error_path
        SidebarWeatherError.Temperature -> R.string.sidebar_weather_error_temperature
        SidebarWeatherError.Json -> R.string.sidebar_weather_error_json
        SidebarWeatherError.TooLarge -> R.string.sidebar_weather_error_large
        SidebarWeatherError.Redirect -> R.string.sidebar_weather_error_redirect
        SidebarWeatherError.Timeout -> R.string.sidebar_weather_error_timeout
        SidebarWeatherError.Tls -> R.string.sidebar_weather_error_tls
        else -> R.string.sidebar_weather_error_network
    })
}
