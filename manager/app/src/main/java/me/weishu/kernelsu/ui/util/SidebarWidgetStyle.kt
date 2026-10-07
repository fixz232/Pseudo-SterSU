package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.edit

enum class SidebarWidgetType(val value: String) {
    Clock("clock"),
    Weather("weather"),
    Alarm("alarm");

    companion object {
        fun fromValue(value: String?): SidebarWidgetType =
            entries.firstOrNull { it.value == value } ?: Clock
    }
}

enum class SidebarClockStyle(val value: String) {
    Stacked("stacked"),
    Compact("compact"),
    DateFirst("date_first");

    companion object {
        fun fromValue(value: String?): SidebarClockStyle =
            entries.firstOrNull { it.value == value } ?: Stacked
    }
}

enum class SidebarImageShape(val value: String) {
    Circle("circle"),
    Square("square"),
    Diamond("diamond"),
    Triangle("triangle");

    companion object {
        fun fromValue(value: String?): SidebarImageShape =
            entries.firstOrNull { it.value == value } ?: Circle
    }
}

enum class SidebarNavigationPosition(val value: String) {
    Top("top"),
    Center("center"),
    Bottom("bottom");

    companion object {
        fun fromValue(value: String?): SidebarNavigationPosition =
            entries.firstOrNull { it.value == value } ?: Center
    }
}

enum class SidebarSide(val value: String) {
    Left("left"),
    Right("right");

    fun isAtStart(layoutDirection: LayoutDirection): Boolean =
        (this == Left) == (layoutDirection == LayoutDirection.Ltr)

    companion object {
        fun fromValue(value: String?): SidebarSide =
            entries.firstOrNull { it.value == value } ?: Left
    }
}

enum class SidebarHomeLayout(val value: String) {
    Material("material"),
    StatusCards("status_cards");

    companion object {
        fun fromValue(value: String?): SidebarHomeLayout =
            entries.firstOrNull { it.value == value } ?: Material
    }
}

const val SIDEBAR_NAV_HOME = "home"
const val SIDEBAR_NAV_KPM = "kpm"
const val SIDEBAR_NAV_SUPERUSER = "superuser"
const val SIDEBAR_NAV_MODULE = "module"
const val SIDEBAR_NAV_SETTINGS = "settings"

val SIDEBAR_NAVIGATION_IDS = listOf(
    SIDEBAR_NAV_HOME,
    SIDEBAR_NAV_KPM,
    SIDEBAR_NAV_SUPERUSER,
    SIDEBAR_NAV_MODULE,
    SIDEBAR_NAV_SETTINGS,
)

data class SidebarWidgetConfig(
    val widgetType: SidebarWidgetType = SidebarWidgetType.Clock,
    val clockStyle: SidebarClockStyle = SidebarClockStyle.Stacked,
    val weatherLabel: String = "",
    val weatherTemperature: String = "",
    // Retain the original image preference keys so existing artwork becomes the avatar.
    val imageUriString: String? = null,
    val imageShape: SidebarImageShape = SidebarImageShape.Circle,
    val navigationPosition: SidebarNavigationPosition = SidebarNavigationPosition.Center,
    val navigationOrder: List<String> = SIDEBAR_NAVIGATION_IDS,
    val side: SidebarSide = SidebarSide.Left,
    val weatherApi: SidebarWeatherConfig = SidebarWeatherConfig(),
    // Retain the old switch for older versions; the explicit material takes precedence.
    val glassEnabled: Boolean = true,
    val material: SidebarMaterial = SidebarMaterial.fromValue(null, glassEnabled),
    val palettes: SidebarPalettes = SidebarPalettes(),
    val homeLayout: SidebarHomeLayout = SidebarHomeLayout.Material,
    val homeCards: SidebarHomeCards = SidebarHomeCards(),
) {
    fun normalized(): SidebarWidgetConfig = copy(
        weatherLabel = weatherLabel.trim().take(MAX_WEATHER_LABEL_LENGTH),
        weatherTemperature = weatherTemperature.trim().take(MAX_WEATHER_TEMPERATURE_LENGTH),
        imageUriString = imageUriString?.trim()?.takeIf(String::isNotEmpty),
        navigationOrder = normalizeSidebarNavigationOrder(navigationOrder),
        weatherApi = weatherApi.normalized(),
        palettes = palettes.normalized(),
        homeCards = homeCards.normalized(),
    )
}

fun normalizeSidebarNavigationOrder(
    requested: List<String>,
    available: List<String> = SIDEBAR_NAVIGATION_IDS,
): List<String> {
    val supported = available.distinct().filter { it in SIDEBAR_NAVIGATION_IDS }
    if (supported.isEmpty()) return emptyList()
    val requestedValid = requested.filter { it in supported }.distinct()
    return requestedValid + supported.filterNot(requestedValid::contains)
}

fun readSidebarWidgetConfig(context: Context): SidebarWidgetConfig {
    val prefs = sidebarWidgetPreferences(context)
    val navigationOrder = prefs.getString(SIDEBAR_NAVIGATION_ORDER_KEY, null)
        ?.split(',')
        .orEmpty()
        .map(String::trim)
        .filter(String::isNotEmpty)
    return SidebarWidgetConfig(
        widgetType = SidebarWidgetType.fromValue(prefs.getString(SIDEBAR_WIDGET_TYPE_KEY, null)),
        clockStyle = SidebarClockStyle.fromValue(prefs.getString(SIDEBAR_CLOCK_STYLE_KEY, null)),
        weatherLabel = prefs.getString(SIDEBAR_WEATHER_LABEL_KEY, "").orEmpty(),
        weatherTemperature = prefs.getString(SIDEBAR_WEATHER_TEMPERATURE_KEY, "").orEmpty(),
        imageUriString = prefs.getString(SIDEBAR_IMAGE_URI_KEY, null),
        imageShape = SidebarImageShape.fromValue(prefs.getString(SIDEBAR_IMAGE_SHAPE_KEY, null)),
        navigationPosition = SidebarNavigationPosition.fromValue(
            prefs.getString(SIDEBAR_NAVIGATION_POSITION_KEY, null)
        ),
        navigationOrder = navigationOrder.ifEmpty { SIDEBAR_NAVIGATION_IDS },
        side = SidebarSide.fromValue(prefs.getString(SIDEBAR_SIDE_KEY, null)),
        weatherApi = readSidebarWeatherConfig(context),
        glassEnabled = prefs.getBoolean(SIDEBAR_GLASS_ENABLED_KEY, true),
        material = SidebarMaterial.fromValue(
            prefs.getString(SIDEBAR_MATERIAL_KEY, null),
            prefs.getBoolean(SIDEBAR_GLASS_ENABLED_KEY, true),
        ),
        palettes = decodeSidebarPalettes(prefs.getString(SIDEBAR_PALETTES_KEY, null)),
        homeLayout = SidebarHomeLayout.fromValue(prefs.getString(SIDEBAR_HOME_LAYOUT_KEY, null)),
        homeCards = decodeSidebarHomeCards(prefs.getString(SIDEBAR_HOME_CARDS_KEY, null)),
    ).normalized()
}

fun writeSidebarWidgetConfig(context: Context, config: SidebarWidgetConfig) {
    val value = config.normalized()
    writeSidebarWeatherConfig(context, value.weatherApi)
    sidebarWidgetPreferences(context).edit(commit = true) {
        putString(SIDEBAR_WIDGET_TYPE_KEY, value.widgetType.value)
        putString(SIDEBAR_CLOCK_STYLE_KEY, value.clockStyle.value)
        putString(SIDEBAR_WEATHER_LABEL_KEY, value.weatherLabel)
        putString(SIDEBAR_WEATHER_TEMPERATURE_KEY, value.weatherTemperature)
        if (value.imageUriString == null) {
            remove(SIDEBAR_IMAGE_URI_KEY)
        } else {
            putString(SIDEBAR_IMAGE_URI_KEY, value.imageUriString)
        }
        putString(SIDEBAR_IMAGE_SHAPE_KEY, value.imageShape.value)
        putString(SIDEBAR_NAVIGATION_POSITION_KEY, value.navigationPosition.value)
        putString(SIDEBAR_NAVIGATION_ORDER_KEY, value.navigationOrder.joinToString(","))
        putString(SIDEBAR_SIDE_KEY, value.side.value)
        putBoolean(SIDEBAR_GLASS_ENABLED_KEY, value.material != SidebarMaterial.Flat)
        putString(SIDEBAR_MATERIAL_KEY, value.material.value)
        putString(SIDEBAR_PALETTES_KEY, encodeSidebarPalettes(value.palettes))
        putString(SIDEBAR_HOME_LAYOUT_KEY, value.homeLayout.value)
        putString(SIDEBAR_HOME_CARDS_KEY, encodeSidebarHomeCards(value.homeCards))
    }
}

@Composable
internal fun rememberSidebarWidgetConfig(): SidebarWidgetConfig {
    val context = LocalContext.current
    val preferences = remember(context) { sidebarWidgetPreferences(context) }
    val weatherPreferences = remember(context) { sidebarWeatherPreferences(context) }
    var config by remember(preferences) { mutableStateOf(readSidebarWidgetConfig(context)) }
    DisposableEffect(preferences, weatherPreferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (isSidebarWidgetPreference(key) || key == SIDEBAR_WEATHER_CONFIG_KEY) config = readSidebarWidgetConfig(context)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        weatherPreferences.registerOnSharedPreferenceChangeListener(listener)
        config = readSidebarWidgetConfig(context)
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
            weatherPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    return config
}

internal fun sidebarWidgetPreferences(context: Context): SharedPreferences =
    context.applicationContext.getSharedPreferences(SIDEBAR_WIDGET_PREFS_NAME, Context.MODE_PRIVATE)

internal fun isSidebarWidgetPreference(key: String?): Boolean = key in SIDEBAR_WIDGET_KEYS

const val SIDEBAR_WIDGET_IMAGE_STORAGE_KEY = "sidebar_widget_custom_image"

private const val SIDEBAR_WIDGET_PREFS_NAME = "settings"
private const val SIDEBAR_WIDGET_TYPE_KEY = "sidebar_widget_type"
private const val SIDEBAR_CLOCK_STYLE_KEY = "sidebar_widget_clock_style"
private const val SIDEBAR_WEATHER_LABEL_KEY = "sidebar_widget_weather_label"
private const val SIDEBAR_WEATHER_TEMPERATURE_KEY = "sidebar_widget_weather_temperature"
private const val SIDEBAR_IMAGE_URI_KEY = "sidebar_widget_image_uri"
private const val SIDEBAR_IMAGE_SHAPE_KEY = "sidebar_widget_image_shape"
private const val SIDEBAR_NAVIGATION_POSITION_KEY = "sidebar_widget_navigation_position"
private const val SIDEBAR_NAVIGATION_ORDER_KEY = "sidebar_widget_navigation_order"
private const val SIDEBAR_SIDE_KEY = "sidebar_widget_side"
private const val SIDEBAR_GLASS_ENABLED_KEY = "sidebar_widget_glass_enabled"
private const val SIDEBAR_HOME_LAYOUT_KEY = "sidebar_widget_home_layout"
private const val MAX_WEATHER_LABEL_LENGTH = 24
private const val MAX_WEATHER_TEMPERATURE_LENGTH = 12
private val SIDEBAR_WIDGET_KEYS = setOf(
    SIDEBAR_WIDGET_TYPE_KEY,
    SIDEBAR_CLOCK_STYLE_KEY,
    SIDEBAR_WEATHER_LABEL_KEY,
    SIDEBAR_WEATHER_TEMPERATURE_KEY,
    SIDEBAR_IMAGE_URI_KEY,
    SIDEBAR_IMAGE_SHAPE_KEY,
    SIDEBAR_NAVIGATION_POSITION_KEY,
    SIDEBAR_NAVIGATION_ORDER_KEY,
    SIDEBAR_SIDE_KEY,
    SIDEBAR_GLASS_ENABLED_KEY,
    SIDEBAR_MATERIAL_KEY,
    SIDEBAR_PALETTES_KEY,
    SIDEBAR_HOME_LAYOUT_KEY,
    SIDEBAR_HOME_CARDS_KEY,
)
