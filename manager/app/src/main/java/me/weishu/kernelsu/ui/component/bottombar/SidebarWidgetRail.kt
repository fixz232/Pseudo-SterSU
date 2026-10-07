package me.weishu.kernelsu.ui.component.bottombar

import android.app.AlarmManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalMainPagerState
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.component.rememberSystemAnimationsEnabled
import me.weishu.kernelsu.ui.component.rememberCustomImageBitmap
import me.weishu.kernelsu.ui.component.rebootlistpopup.SidebarRebootPopup
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.screen.flash.FlashIt
import me.weishu.kernelsu.ui.screen.module.rememberModuleZipPicker
import me.weishu.kernelsu.ui.viewmodel.ModuleViewModel
import me.weishu.kernelsu.ui.util.LocalCustomNavigationIcons
import me.weishu.kernelsu.ui.util.SIDEBAR_NAV_HOME
import me.weishu.kernelsu.ui.util.SIDEBAR_NAV_KPM
import me.weishu.kernelsu.ui.util.SIDEBAR_NAV_MODULE
import me.weishu.kernelsu.ui.util.SIDEBAR_NAV_SETTINGS
import me.weishu.kernelsu.ui.util.SIDEBAR_NAV_SUPERUSER
import me.weishu.kernelsu.ui.util.SidebarClockStyle
import me.weishu.kernelsu.ui.util.SidebarImageShape
import me.weishu.kernelsu.ui.util.SidebarNavigationPosition
import me.weishu.kernelsu.ui.util.SidebarSide
import me.weishu.kernelsu.ui.util.SidebarMaterial
import me.weishu.kernelsu.ui.util.SidebarWidgetConfig
import me.weishu.kernelsu.ui.util.SidebarWidgetType
import me.weishu.kernelsu.ui.util.normalizeSidebarNavigationOrder
import me.weishu.kernelsu.ui.util.rememberSidebarWidgetConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class SidebarAppearance(
    val colors: SidebarColors = sidebarColors(dark = false),
    val animations: Boolean = false,
    val material: SidebarMaterial = SidebarMaterial.Flat,
)

private val LocalSidebarAppearance = staticCompositionLocalOf { SidebarAppearance() }

@Composable
fun SidebarWidgetRail(
    navigationBadge: NavigationBadgeState,
    destinations: List<MainDestination>,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val tablet = configuration.smallestScreenWidthDp >= 600
    val dark = isInDarkTheme()
    val mainPagerState = LocalMainPagerState.current
    val customIcons = LocalCustomNavigationIcons.current
    val config = rememberSidebarWidgetConfig()
    val navigator = LocalNavigator.current
    val moduleViewModel = viewModel<ModuleViewModel>()
    val moduleUiState by moduleViewModel.uiState.collectAsStateWithLifecycle()
    val showModuleInstall = mainPagerState.destinationForPage() == MainDestination.Module &&
        moduleUiState.installButtonVisible
    val openModuleZipPicker = rememberModuleZipPicker { uris ->
        if (uris.isNotEmpty()) {
            navigator.push(Route.Flash(FlashIt.FlashModules(uris)))
            moduleViewModel.markNeedRefresh()
        }
    }
    val orderedDestinations = remember(config.navigationOrder, destinations) {
        orderSidebarDestinations(config.navigationOrder, destinations)
    }
    SidebarRailLayout(
        config = config,
        navigationCount = orderedDestinations.size + if (showModuleInstall) 1 else 0,
        tablet = tablet,
        modifier = modifier,
        onAvatarClick = { navigator.push(Route.SidebarWidgetSettings) },
        footer = { SidebarRebootPopup() },
        windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
            (if (config.side == SidebarSide.Left) WindowInsetsSides.Left else WindowInsetsSides.Right) +
                WindowInsetsSides.Vertical
        ),
    ) {
        orderedDestinations.forEach { destination ->
            val selected = mainPagerState.destinationForPage() == destination
            val label = customIcons.labelFor(destination, stringResource(destination.label))
            val badge = badgeFor(destination, navigationBadge)
            val contentColor = LocalContentColor.current
            SidebarRailItem(
                selected = selected,
                label = label,
                badgeCount = badge?.count,
                onClick = { mainPagerState.animateTo(destination) },
            ) {
                BadgedBox(badge = {
                    if (badge != null) {
                        Badge(
                            containerColor = contentColor,
                            contentColor = sidebarColors(config.palettes.palette(config.material, dark)).background,
                        ) {
                            Text(badge.count.toString())
                        }
                    }
                }) {
                    NavigationDestinationIcon(
                        destination = destination,
                        state = customIcons.stateFor(destination),
                        contentDescription = null,
                        tint = contentColor,
                    )
                }
            }
            if (destination == MainDestination.Module && showModuleInstall) {
                SidebarRailItem(
                    selected = false,
                    label = stringResource(R.string.module_install),
                    onClick = openModuleZipPicker,
                    role = Role.Button,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

/** Shared by the live rail and its settings preview. */
@Composable
internal fun SidebarRailLayout(
    config: SidebarWidgetConfig,
    navigationCount: Int,
    modifier: Modifier = Modifier,
    tablet: Boolean = false,
    windowInsets: WindowInsets = WindowInsets(0, 0, 0, 0),
    onAvatarClick: (() -> Unit)? = null,
    dark: Boolean = isInDarkTheme(),
    showWidgets: Boolean = true,
    footer: @Composable () -> Unit = {
        Icon(Icons.Rounded.PowerSettingsNew, contentDescription = stringResource(R.string.reboot), modifier = Modifier.size(30.dp))
    },
    navigation: @Composable ColumnScope.() -> Unit,
) {
    val colors = sidebarColors(config.palettes.palette(config.material, dark))
    val backdrop = LocalSidebarGlassBackdrop.current
    val mode = sidebarGlassMode(
        config.material, Build.VERSION.SDK_INT,
        LocalView.current.isHardwareAccelerated && !LocalInspectionMode.current, backdrop != null,
    )
    val contentColor = colors.content
    val animations = rememberSystemAnimationsEnabled() && !LocalInspectionMode.current
    val appearance = remember(colors, animations, config.material) {
        SidebarAppearance(colors, animations, config.material)
    }
    val railWidth = (if (tablet) 96.dp else 80.dp) * LocalDensity.current.fontScale.coerceIn(1f, 1.2f)
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalSidebarAppearance provides appearance,
    ) {
        Column(
            modifier = modifier
                .clip(SidebarPaneShape)
                .sidebarMaterialSurface(colors, config.material, mode, backdrop, edgeOnRight = config.side == SidebarSide.Left)
                .windowInsetsPadding(windowInsets)
                .width(railWidth)
                .fillMaxHeight(),
        ) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val minimumWidgetHeight = if (showWidgets) sidebarMinimumWidgetHeight(
                    config.widgetType, tablet, LocalDensity.current.fontScale,
                ) else 0.dp
                val (contentHeight, widgetHeight) = sidebarRailSizing(
                    maxHeight, minimumWidgetHeight, navigationCount, config.navigationPosition,
                    fontScale = LocalDensity.current.fontScale,
                )
                // The body scrolls on short windows; the power button stays reachable below it.
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Column(
                        modifier = Modifier.fillMaxWidth().height(contentHeight),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (showWidgets) Column(
                            Modifier.fillMaxWidth().height(widgetHeight).padding(top = 16.dp, bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            SidebarAvatar(config, compact = !tablet, onClick = onAvatarClick)
                            Spacer(Modifier.height(12.dp))
                            SidebarHeaderWidget(config, modifier = Modifier.weight(1f).padding(horizontal = 6.dp), compact = !tablet)
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = contentColor.copy(alpha = 0.22f))
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth().selectableGroup(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = when (config.navigationPosition) {
                                SidebarNavigationPosition.Top -> Arrangement.Top
                                SidebarNavigationPosition.Center -> Arrangement.SpaceEvenly
                                SidebarNavigationPosition.Bottom -> Arrangement.Bottom
                            },
                            content = navigation,
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = contentColor.copy(alpha = 0.22f))
            Box(
                modifier = Modifier.fillMaxWidth().height(76.dp),
                contentAlignment = Alignment.Center,
            ) { footer() }
        }
    }
}

@Composable
internal fun SidebarRailItem(
    selected: Boolean,
    label: String,
    badgeCount: Int? = null,
    onClick: (() -> Unit)? = null,
    role: Role = Role.Tab,
    content: @Composable () -> Unit,
) {
    val appearance = LocalSidebarAppearance.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val selection by animateFloatAsState(
        targetValue = when {
            pressed -> 1f
            selected -> 0.85f
            focused -> 0.7f
            hovered -> 0.45f
            else -> 0f
        },
        animationSpec = tween(if (appearance.animations) 180 else 0, easing = FastOutSlowInEasing),
        label = "sidebarSelection",
    )
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .fillMaxWidth()
            .height(sidebarNavigationItemHeight(LocalDensity.current.fontScale) - 4.dp)
            .clip(SidebarPaneShape)
            .sidebarSelectionSurface(appearance.colors, appearance.material, selection)
            .semantics(mergeDescendants = true) {
                contentDescription = if (badgeCount != null) "$label, $badgeCount" else label
            }
            .then(
                if (focused) Modifier.border(
                    1.5.dp,
                    appearance.colors.content,
                    SidebarPaneShape,
                ) else Modifier
            )
            .then(
                when {
                    onClick == null -> Modifier
                    role == Role.Button -> Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                    )
                    else -> Modifier.selectable(
                        selected = selected,
                        interactionSource = interaction,
                        indication = null,
                        role = role,
                        onClick = onClick,
                    )
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp).clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            content()
            Text(
                text = label,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val now by produceState(initialValue = System.currentTimeMillis(), config.widgetType, lifecycleOwner) {
        if (config.widgetType == SidebarWidgetType.Clock || config.widgetType == SidebarWidgetType.Alarm) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    value = System.currentTimeMillis()
                    delay(60_000L - value % 60_000L)
                }
            }
        }
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (config.widgetType) {
            SidebarWidgetType.Clock -> SidebarClockWidget(config, now, compact)
            SidebarWidgetType.Weather -> SidebarWeatherWidget(config, compact)
            SidebarWidgetType.Alarm -> SidebarAlarmWidget(context, now, compact)
        }
    }
}

@Composable
private fun SidebarClockWidget(config: SidebarWidgetConfig, now: Long, compact: Boolean) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val timePattern = if (DateFormat.is24HourFormat(LocalContext.current)) "HH:mm" else "h:mm"
    val time = remember(now / 1_000L, locale, timePattern) {
        SimpleDateFormat(timePattern, locale).format(Date(now))
    }
    val date = remember(now / 60_000L, locale) {
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "MMMd"), locale).format(Date(now))
    }
    val shortDate = remember(now / 60_000L, locale) {
        SimpleDateFormat("M/d", locale).format(Date(now))
    }
    val weekday = remember(now / 60_000L, locale) {
        SimpleDateFormat("EEE", locale).format(Date(now))
    }
    val battery = remember(now / 60_000L, context) {
        context.getSystemService(BatteryManager::class.java)
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when (config.clockStyle) {
            SidebarClockStyle.Stacked -> {
                val parts = time.split(':', limit = 2)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(parts.firstOrNull().orEmpty(), fontSize = if (compact) 30.sp else 36.sp, lineHeight = if (compact) 34.sp else 40.sp, fontWeight = FontWeight.SemiBold)
                    Text(parts.getOrNull(1).orEmpty(), fontSize = if (compact) 30.sp else 36.sp, lineHeight = if (compact) 34.sp else 40.sp, fontWeight = FontWeight.SemiBold)
                    Text("$date $weekday", fontSize = if (compact) 10.sp else 12.sp, lineHeight = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 2)
                }
            }

            SidebarClockStyle.Compact -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = time,
                    fontSize = if (compact) 18.sp else 22.sp,
                    lineHeight = if (compact) 22.sp else 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(date, fontSize = if (compact) 11.sp else 13.sp, lineHeight = 16.sp)
                Text(weekday, fontSize = if (compact) 10.sp else 12.sp, lineHeight = 16.sp)
            }

            SidebarClockStyle.DateFirst -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(weekday, fontSize = if (compact) 11.sp else 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(shortDate, fontSize = if (compact) 22.sp else 28.sp, lineHeight = if (compact) 26.sp else 32.sp, fontWeight = FontWeight.Bold)
                Text(time, fontSize = if (compact) 14.sp else 17.sp, lineHeight = if (compact) 18.sp else 21.sp)
            }
        }
        if (battery != null) Text("$battery%", fontSize = if (compact) 11.sp else 12.sp, lineHeight = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.SemiBold)
        SidebarWeatherSummary(config, compact)
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
            lineHeight = if (compact) 24.sp else 28.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(
                if (nextAlarm == null) R.string.sidebar_widget_alarm_none
                else R.string.sidebar_widget_alarm_next
            ),
            fontSize = if (compact) 9.sp else 11.sp,
            lineHeight = if (compact) 12.sp else 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
internal fun SidebarAvatar(
    config: SidebarWidgetConfig,
    compact: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val bitmap = rememberCustomImageBitmap(config.imageUriString, maxSide = 512)
    val size = if (compact) 60.dp else 76.dp
    val shape = remember(config.imageShape) { sidebarImageShape(config.imageShape) }
    val label = stringResource(R.string.sidebar_avatar_title)
    val contentColor = LocalContentColor.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(LocalSidebarAppearance.current.colors.selection)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap == null) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = contentColor, modifier = Modifier.size(size * 0.48f))
        } else {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
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
    SidebarImageShape.Square -> RectangleShape
    SidebarImageShape.Diamond -> PolygonShape(listOf(0.5f to 0f, 1f to 0.5f, 0.5f to 1f, 0f to 0.5f))
    SidebarImageShape.Triangle -> PolygonShape(listOf(0.5f to 0f, 1f to 1f, 0f to 1f))
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
