package me.weishu.kernelsu.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Shared chrome for the sidebar style; status/action colors remain user-selected. */
object SidebarUiTokens {
    val PagePadding = 16.dp
    val SectionSpacing = 16.dp
    val CardRadius = 16.dp
    val ContentMaxWidth = 1120.dp
    val DetailMaxWidth = 880.dp
    val Shapes = Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(CardRadius),
        extraLarge = RoundedCornerShape(20.dp),
    )
}

/** Neutral surfaces keep wallpaper and semantic colors from competing with page content. */
internal fun sidebarColorScheme(base: ColorScheme, dark: Boolean, amoled: Boolean): ColorScheme {
    val background = if (dark) {
        if (amoled) Color.Black else Color(0xFF141414)
    } else Color(0xFFF5F5F5)
    val ink = if (dark) Color(0xFFF3F3F3) else Color(0xFF1C1C1C)
    return base.copy(
        background = background,
        onBackground = ink,
        surface = background,
        onSurface = ink,
        surfaceDim = if (dark) Color(0xFF101010) else Color(0xFFE5E5E5),
        surfaceBright = if (dark) Color(0xFF292929) else Color.White,
        surfaceContainerLowest = if (dark) Color(0xFF101010) else Color.White,
        surfaceContainerLow = if (dark) Color(0xFF1C1C1C) else Color(0xFFFAFAFA),
        surfaceContainer = if (dark) Color(0xFF222222) else Color.White,
        surfaceContainerHigh = if (dark) Color(0xFF2C2C2C) else Color(0xFFEBEBEB),
        surfaceContainerHighest = if (dark) Color(0xFF363636) else Color(0xFFE2E2E2),
        surfaceVariant = if (dark) Color(0xFF363636) else Color(0xFFE2E2E2),
        onSurfaceVariant = if (dark) Color(0xFFC8C8C8) else Color(0xFF555555),
        outline = if (dark) Color(0xFF959595) else Color(0xFF747474),
        outlineVariant = if (dark) Color(0xFF404040) else Color(0xFFD6D6D6),
        inverseSurface = if (dark) Color(0xFFF3F3F3) else Color(0xFF272727),
        inverseOnSurface = if (dark) Color(0xFF1C1C1C) else Color(0xFFF3F3F3),
    )
}
