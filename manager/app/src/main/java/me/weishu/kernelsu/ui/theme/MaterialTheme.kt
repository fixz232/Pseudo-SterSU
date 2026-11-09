package me.weishu.kernelsu.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.rain.LocalRainSettings
import me.weishu.kernelsu.ui.component.rain.withRainAppearance
import me.weishu.kernelsu.ui.util.AppFontState
import me.weishu.kernelsu.ui.util.resolveAppFontFamily
import me.weishu.kernelsu.ui.webui.MonetColorsProvider
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalContentColor as MiuixLocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun MaterialKernelSUTheme(
    appSettings: AppSettings,
    appFontState: AppFontState,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = appSettings.colorMode.isDark || (appSettings.colorMode.isSystem && systemDarkTheme)
    val amoledMode = appSettings.colorMode.isAmoled
    val rain = LocalRainSettings.current
    val seedColor = if (rain.active) (if (darkTheme) rain.darkAccent else rain.lightAccent) ?: rain.scene.keyColor
        else appSettings.keyColor
    val dynamicColor = seedColor == 0

    val colorScheme = rememberKernelSUColorScheme(
        seedColor = if (dynamicColor) Color.Unspecified else Color(seedColor),
        isDark = darkTheme,
        isAmoled = amoledMode,
        paletteStyle = appSettings.paletteStyle,
        colorSpec = appSettings.colorSpec,
    )

    LaunchedEffect(darkTheme) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val sidebar = LocalInterfaceStyle.current == InterfaceStyle.SidebarWidget.value
    val effectiveColorScheme = if (sidebar) {
        sidebarColorScheme(colorScheme, dark = darkTheme, amoled = amoledMode)
    } else colorScheme.withMonetSurfaceOpacity(appSettings.monetSurfaceOpacity)
    val animatedColorScheme = effectiveColorScheme.withRainAppearance(rain, darkTheme).animateAsState()
    val appFontFamily = remember(appFontState) {
        resolveAppFontFamily(context, appFontState)
    }
    val materialTypography = remember(appFontFamily) {
        Typography(fontFamily = appFontFamily)
    }
    // Material/sidebar screens also host native Miuix preferences and cards.
    // Supply their palette before UiPassthroughTheme records the original colors.
    val miuixController = remember(darkTheme, effectiveColorScheme.primary) {
        ThemeController(
            if (darkTheme) ColorSchemeMode.Dark else ColorSchemeMode.Light,
            keyColor = effectiveColorScheme.primary,
            isDark = darkTheme,
        )
    }

    MaterialExpressiveTheme(
        colorScheme = animatedColorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = materialTypography,
        shapes = if (sidebar) SidebarUiTokens.Shapes else null,
        content = {
            MiuixTheme(controller = miuixController) {
                val contentColor = animatedColorScheme.onBackground.copy(alpha = appFontState.opacity)
                CompositionLocalProvider(
                    LocalContentColor provides contentColor,
                    MiuixLocalContentColor provides contentColor,
                ) {
                    MonetColorsProvider.UpdateCss()
                    content()
                }
            }
        }
    )
}
