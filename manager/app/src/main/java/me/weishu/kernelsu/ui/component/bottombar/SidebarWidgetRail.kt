package me.weishu.kernelsu.ui.component.bottombar

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalMainPagerState
import me.weishu.kernelsu.ui.component.rebootlistpopup.RebootListPopupMaterial
import me.weishu.kernelsu.ui.component.rememberCustomImageBitmap
import me.weishu.kernelsu.ui.util.LocalCustomNavigationIcons
import me.weishu.kernelsu.ui.util.SIDEBAR_NAV_HOME
import me.weishu.kernelsu.ui.util.SIDEBAR_NAV_KPM
import me.weishu.kernelsu.ui.util.SIDEBAR_NAV_MODULE
import me.weishu.kernelsu.ui.util.SIDEBAR_NAV_SETTINGS
import me.weishu.kernelsu.ui.util.SIDEBAR_NAV_SUPERUSER
import me.weishu.kernelsu.ui.util.SidebarClockStyle
import me.weishu.kernelsu.ui.util.SidebarImageShape
import me.weishu.kernelsu.ui.util.SidebarNavigationPosition
import me.weishu.kernelsu.ui.util.SidebarWidgetConfig
import me.weishu.kernelsu.ui.util.SidebarWidgetType
import me.weishu.kernelsu.ui.util.isSidebarWidgetPreference
import me.weishu.kernelsu.ui.util.normalizeSidebarNavigationOrder
import me.weishu.kernelsu.ui.util.readSidebarWidgetConfig
import me.weishu.kernelsu.ui.util.sidebarWidgetPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SidebarWidgetRail(
    navigationBadge: NavigationBadgeState,
    destinations: List<MainDestination>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val tablet = configuration.smallestScreenWidthDp >= 600
    val width = if (tablet) 104.dp else 76.dp
    val dark = isSystemInDarkTheme()
    val mainPagerState = LocalMainPagerState.current
    val customIcons = LocalCustomNavigationIcons.current
    val config = rememberSidebarWidgetConfig(context)
    val orderedDestinations = remember(config.navigationOrder, destinations) {
        orderSidebarDestinations(config.navigationOrder, destinations)
    }
    val gradient = remember(dark) {
        if (dark) {
            Brush.verticalGradient(listOf(Color(0xFF23334C), Color(0xFF36455A), Color(0xFF232B37)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFF73A8F0), Color(0xFFBFD0E5), Color(0xFFE1E2E4)))
        }
    }
    val contentColor = if (dark) Color(0xFFF3F6FA) else Color(0xFF101820)

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Column(
            modifier = modifier
                .width(width)
                .fillMaxHeight()
                .background(gradient)
                .windowInsetsPadding(
                    WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
                        WindowInsetsSides.Start + WindowInsetsSides.Vertical
                    )
                )
                .padding(horizontal = if (tablet) 12.dp else 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                RebootListPopupMaterial()
            }
            Spacer(Modifier.height(if (tablet) 12.dp else 5.dp))
            SidebarHeaderWidget(
                config = config,
                compact = !tablet,
                modifier = Modifier.height(if (tablet) 174.dp else 148.dp),
            )
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = when (config.navigationPosition) {
                        SidebarNavigationPosition.Top -> Arrangement.Top
                        SidebarNavigationPosition.Center -> Arrangement.Center
                        SidebarNavigationPosition.Bottom -> Arrangement.Bottom
                    },
                ) {
                    orderedDestinations.forEach { destination ->
                        val selected = mainPagerState.destinationForPage() == destination
                        val label = customIcons.labelFor(destination, stringResource(destination.label))
                        Box(
                            modifier = Modifier
                                .padding(vertical = if (tablet) 4.dp else 2.dp)
                                .size(if (tablet) 58.dp else 52.dp)
                                .clip(RoundedCornerShape(if (tablet) 22.dp else 19.dp))
                                .background(
                                    if (selected) {
                                        if (dark) Color.White.copy(alpha = 0.19f)
                                        else Color.White.copy(alpha = 0.67f)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable(
                                    role = Role.Tab,
                                    onClick = { mainPagerState.animateTo(destination) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            CompositionLocalProvider(
                                LocalContentColor provides if (selected) {
                                    if (dark) Color.White else Color(0xFF0A1724)
                                } else {
                                    contentColor.copy(alpha = 0.88f)
                                }
                            ) {
                                NavigationIconWithBadge(
                                    destination = destination,
                                    state = customIcons.stateFor(destination),
                                    contentDescription = label,
                                    badge = badgeFor(destination, navigationBadge),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SidebarHeaderWidget(
    config: SidebarWidgetConfig,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
) {
    val context = LocalContext.current
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (isActive) {
            value = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val contentColor = LocalContentColor.current
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (config.widgetType) {
            SidebarWidgetType.Clock -> SidebarClockWidget(config.clockStyle, now, compact)
            SidebarWidgetType.Weather -> SidebarWeatherWidget(config, compact)
            SidebarWidgetType.Alarm -> SidebarAlarmWidget(context, now, compact)
            SidebarWidgetType.Image -> SidebarImageWidget(config, compact, contentColor)
        }
    }
}

@Composable
private fun SidebarClockWidget(style: SidebarClockStyle, now: Long, compact: Boolean) {
    val locale = Locale.getDefault()
    val timePattern = if (DateFormat.is24HourFormat(LocalContext.current)) "HH:mm" else "h:mm"
    val time = remember(now / 1_000L, locale, timePattern) {
        SimpleDateFormat(timePattern, locale).format(Date(now))
    }
    val date = remember(now / 60_000L, locale) {
        SimpleDateFormat("M/d", locale).format(Date(now))
    }
    val weekday = remember(now / 60_000L, locale) {
        SimpleDateFormat("EEE", locale).format(Date(now))
    }
    when (style) {
        SidebarClockStyle.Stacked -> {
            val parts = time.split(':', limit = 2)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(parts.firstOrNull().orEmpty(), fontSize = if (compact) 31.sp else 38.sp, lineHeight = 34.sp)
                Text(parts.getOrNull(1).orEmpty(), fontSize = if (compact) 31.sp else 38.sp, lineHeight = 34.sp)
                Text(date, fontSize = if (compact) 11.sp else 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        SidebarClockStyle.Compact -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = time,
                fontSize = if (compact) 18.sp else 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(date, fontSize = if (compact) 11.sp else 13.sp)
            Text(weekday, fontSize = if (compact) 10.sp else 12.sp)
        }

        SidebarClockStyle.DateFirst -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(weekday, fontSize = if (compact) 11.sp else 13.sp, fontWeight = FontWeight.SemiBold)
            Text(date, fontSize = if (compact) 22.sp else 28.sp, fontWeight = FontWeight.Bold)
            Text(time, fontSize = if (compact) 14.sp else 17.sp)
        }
    }
}

@Composable
private fun SidebarWeatherWidget(config: SidebarWidgetConfig, compact: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.Cloud, contentDescription = null, modifier = Modifier.size(if (compact) 27.dp else 34.dp))
        Text(
            text = config.weatherTemperature.ifBlank { stringResource(R.string.sidebar_widget_weather_default_temperature) },
            fontSize = if (compact) 20.sp else 25.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            text = config.weatherLabel.ifBlank { stringResource(R.string.sidebar_widget_weather_default_label) },
            fontSize = if (compact) 10.sp else 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SidebarAlarmWidget(context: Context, now: Long, compact: Boolean) {
    val nextAlarm = remember(now / 60_000L) {
        context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime
    }
    val locale = Locale.getDefault()
    val formatted = nextAlarm?.let { trigger ->
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
        remember(trigger, locale, pattern) { SimpleDateFormat(pattern, locale).format(Date(trigger)) }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.Alarm, contentDescription = null, modifier = Modifier.size(if (compact) 26.dp else 32.dp))
        Text(
            text = formatted ?: "--:--",
            fontSize = if (compact) 19.sp else 24.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(
                if (nextAlarm == null) R.string.sidebar_widget_alarm_none
                else R.string.sidebar_widget_alarm_next
            ),
            fontSize = if (compact) 9.sp else 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun SidebarImageWidget(
    config: SidebarWidgetConfig,
    compact: Boolean,
    contentColor: Color,
) {
    val bitmap = rememberCustomImageBitmap(config.imageUriString, maxSide = 512)
    val size = if (compact) 54.dp else 72.dp
    val shape = remember(config.imageShape) { sidebarImageShape(config.imageShape) }
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(contentColor.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap == null) {
            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(size * 0.44f))
        } else {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.sidebar_widget_custom_image),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun rememberSidebarWidgetConfig(context: Context): SidebarWidgetConfig {
    var config by remember(context) { mutableStateOf(readSidebarWidgetConfig(context)) }
    val preferences = remember(context) { sidebarWidgetPreferences(context) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (isSidebarWidgetPreference(key)) config = readSidebarWidgetConfig(context)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return config
}

internal fun orderSidebarDestinations(
    requested: List<String>,
    destinations: List<MainDestination>,
): List<MainDestination> {
    val destinationsById = destinations.associateBy(::sidebarNavigationId)
    val orderedIds = normalizeSidebarNavigationOrder(requested, destinationsById.keys.toList())
    return orderedIds.mapNotNull(destinationsById::get)
}

internal fun sidebarNavigationId(destination: MainDestination): String = when (destination) {
    MainDestination.Home -> SIDEBAR_NAV_HOME
    MainDestination.Kpm -> SIDEBAR_NAV_KPM
    MainDestination.SuperUser -> SIDEBAR_NAV_SUPERUSER
    MainDestination.Module -> SIDEBAR_NAV_MODULE
    MainDestination.Settings -> SIDEBAR_NAV_SETTINGS
}

private fun sidebarImageShape(shape: SidebarImageShape): Shape = when (shape) {
    SidebarImageShape.Circle -> androidx.compose.foundation.shape.CircleShape
    SidebarImageShape.Square -> RoundedCornerShape(0.dp)
    SidebarImageShape.Diamond -> PolygonShape(listOf(0.5f to 0f, 1f to 0.5f, 0.5f to 1f, 0f to 0.5f))
    SidebarImageShape.Triangle -> PolygonShape(listOf(0.5f to 0f, 1f to 1f, 0f to 1f))
    SidebarImageShape.Star -> StarShape
}

private class PolygonShape(private val points: List<Pair<Float, Float>>) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        points.forEachIndexed { index, (x, y) ->
            val offset = Offset(size.width * x, size.height * y)
            if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

private data object StarShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outer = minOf(size.width, size.height) / 2f
        val inner = outer * 0.44f
        val path = Path()
        repeat(10) { index ->
            val radius = if (index % 2 == 0) outer else inner
            val angle = -PI / 2.0 + index * PI / 5.0
            val x = center.x + (cos(angle) * radius).toFloat()
            val y = center.y + (sin(angle) * radius).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}
