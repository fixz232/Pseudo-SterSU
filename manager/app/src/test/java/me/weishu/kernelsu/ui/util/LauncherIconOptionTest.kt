package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherIconOptionTest {
    @Test
    fun exposesOnlySupportedLauncherIcons() {
        assertEquals(
            listOf(
                LauncherIconOption.Default,
                LauncherIconOption.FoxMask,
                LauncherIconOption.OperaMask,
                LauncherIconOption.SkRoot,
                LauncherIconOption.GridMark,
            ),
            LauncherIconOption.entries,
        )
    }

    @Test
    fun removedLauncherIconsFallBackToDefault() {
        listOf(
            "module",
            "anykernel",
            "neko_star",
            "anime_blue_hair",
            "anime_eyepatch",
            "anime_blonde",
            "anime_white_hair",
            "anime_pink_hair",
        ).forEach { value ->
            assertEquals(LauncherIconOption.Default, LauncherIconOption.fromValue(value))
        }
    }
}
