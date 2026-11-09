package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import me.weishu.kernelsu.ui.component.passthrough.MaterialIcon as Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import me.weishu.kernelsu.ui.component.passthrough.MaterialText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import me.weishu.kernelsu.ui.component.rain.rainNavigationIndicator
import me.weishu.kernelsu.ui.component.rain.RainChromeArea
import me.weishu.kernelsu.ui.component.rain.rainChromeDecoration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import me.weishu.kernelsu.ui.theme.uiPassthroughNavigationColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalMainPagerState
import me.weishu.kernelsu.ui.component.liquid.lens
import me.weishu.kernelsu.ui.component.liquid.vibrancy
import me.weishu.kernelsu.ui.theme.LocalBlurIntensity
import me.weishu.kernelsu.ui.theme.LocalEnableFloatingBottomBar
import me.weishu.kernelsu.ui.theme.LocalEnableFloatingBottomBarBlur
import me.weishu.kernelsu.ui.theme.LocalImmersiveBackgroundActive
import me.weishu.kernelsu.ui.theme.LocalUiPassthroughSettings
import me.weishu.kernelsu.ui.util.BlurredBar
import me.weishu.kernelsu.ui.component.CustomNavigationIconImage
import me.weishu.kernelsu.ui.util.CustomNavigationIconState
import me.weishu.kernelsu.ui.util.LocalCustomNavigationIcons
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported

@Composable
fun BottomBarMaterial(
    navigationBadge: NavigationBadgeState,
    destinations: List<MainDestination>,
    blurBackdrop: LayerBackdrop? = null,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
) {
    // BottomBar already gates visibility through the pager's debounced root state.
    // A second synchronous probe here can tear down navigation during a refresh.
    val floating = LocalEnableFloatingBottomBar.current
    val immersive = LocalImmersiveBackgroundActive.current
    val passthrough = LocalUiPassthroughSettings.current
    val blurIntensity = LocalBlurIntensity.current
    val glass = floating && LocalEnableFloatingBottomBarBlur.current && isRenderEffectSupported() && backdrop != null &&
        !immersive && !passthrough.enabled
    val containerColor = uiPassthroughNavigationColor(MaterialTheme.colorScheme.surfaceContainer)
    val insets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
    )

    if (floating) {
        val shape = RoundedCornerShape(28.dp)
        Box(
            modifier = modifier.fillMaxWidth().windowInsetsPadding(insets)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Box(
                Modifier.clip(shape).then(
                    if (glass) Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            vibrancy()
                            blur(8.dp.toPx() * blurIntensity, 8.dp.toPx() * blurIntensity)
                            lens(refractionHeight = 16.dp.toPx(), refractionAmount = 12.dp.toPx())
                        },
                        onDrawSurface = { drawRect(containerColor.copy(alpha = containerColor.alpha * 0.65f)) },
                    ) else Modifier,
                ),
            ) {
                MaterialNavigationBar(
                    navigationBadge, destinations,
                    containerColor = if (glass) Color.Transparent else containerColor,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                )
            }
        }
    } else {
        Box(modifier) {
            val activeBackdrop = if (passthrough.enabled || immersive) null else blurBackdrop
            BlurredBar(activeBackdrop) {
                MaterialNavigationBar(
                    navigationBadge, destinations,
                    containerColor = if (activeBackdrop != null) Color.Transparent else containerColor,
                    windowInsets = insets,
                )
            }
        }
    }
}

@Composable
private fun MaterialNavigationBar(
    navigationBadge: NavigationBadgeState,
    destinations: List<MainDestination>,
    containerColor: Color,
    windowInsets: WindowInsets,
) {
    val mainPagerState = LocalMainPagerState.current
    val customIcons = LocalCustomNavigationIcons.current
    ShortNavigationBar(
        // Material3 alpha28 constrains each item with the bar's incoming minimum width.
        // Keep the bar full width, but relax that minimum before it divides the items.
        modifier = Modifier.fillMaxWidth().wrapContentWidth()
            .rainChromeDecoration(RainChromeArea.Navigation),
        containerColor = containerColor,
        windowInsets = windowInsets,
        arrangement = ShortNavigationBarArrangement.EqualWeight,
    ) {
        destinations.forEachIndexed { index, destination ->
            val selected = mainPagerState.selectedPage == index
            val label = customIcons.labelFor(destination, stringResource(destination.label))
            val indicatorColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                label = "NavigationIndicatorColor",
            )
            ShortNavigationBarItem(
                modifier = Modifier.rainNavigationIndicator(RoundedCornerShape(24.dp), paintBackground = false, interactionKey = destination, selected = selected),
                selected = selected,
                colors = ShortNavigationBarItemDefaults.colors(selectedIndicatorColor = Color.Transparent),
                onClick = {
                    if (!selected) {
                        mainPagerState.animateToPage(index)
                    }
                },
                icon = {
                    Box(
                        // Paint a fixed capsule without changing icon, label or badge layout.
                        modifier = Modifier.drawBehind {
                            val width = 64.dp.toPx()
                            val height = 32.dp.toPx()
                            drawRoundRect(
                                color = indicatorColor,
                                topLeft = Offset((size.width - width) / 2f, (size.height - height) / 2f),
                                size = Size(width, height),
                                cornerRadius = CornerRadius(height / 2f),
                            )
                        },
                    ) {
                        NavigationIconWithBadge(
                            destination = destination,
                            state = customIcons.stateFor(destination),
                            contentDescription = label,
                            badge = badgeFor(destination, navigationBadge),
                        )
                    }
                },
                label = {
                    Text(
                        label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Composable
internal fun NavigationIconWithBadge(
    destination: MainDestination,
    state: CustomNavigationIconState,
    contentDescription: String?,
    badge: NavBadge?,
) {
    @Composable
    fun navigationIcon() {
        NavigationDestinationIcon(
            destination = destination,
            state = state,
            contentDescription = contentDescription,
            tint = LocalContentColor.current,
        )
    }

    if (badge != null) {
        BadgedBox(
            badge = {
                when (badge.tone) {
                    BadgeTone.Alert -> Badge {
                        Text(badge.count.toString())
                    }

                    BadgeTone.Accent -> Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Text(badge.count.toString())
                    }
                }
            }
        ) {
            navigationIcon()
        }
    } else {
        navigationIcon()
    }
}

@Composable
internal fun NavigationDestinationIcon(
    destination: MainDestination,
    state: CustomNavigationIconState,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier.size(24.dp),
    alpha: Float = 1f,
) {
    CustomNavigationIconImage(
        state = state,
        contentDescription = contentDescription,
        modifier = modifier,
        alpha = alpha,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier,
        )
    }
}
