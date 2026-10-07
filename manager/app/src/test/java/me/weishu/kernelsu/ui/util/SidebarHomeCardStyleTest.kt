package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SidebarHomeCardStyleTest {
    @Test
    fun oldSettingsKeepTheOriginalFinishAndWallpaper() {
        val cards = SidebarWidgetConfig().homeCards
        assertEquals(SidebarHomeCards(), cards)
        SidebarHomeCardId.entries.forEach { id ->
            for (dark in listOf(false, true)) {
                assertEquals(SidebarCardMaterial.Default, cards.style(id, dark).material)
                assertTrue(cards.style(id, dark).inheritImage)
                assertNull(cards.style(id, dark).imageUri)
            }
        }
        assertFalse(cards.needsBackdrop(false))
        assertFalse(cards.needsBackdrop(true))
    }

    @Test
    fun allCardThemePairsRoundTripIndependently() {
        var cards = SidebarHomeCards()
        SidebarHomeCardId.entries.forEachIndexed { index, id ->
            for (dark in listOf(false, true)) {
                cards = cards.withStyle(id, dark, defaultSidebarHomeCardStyle(dark).copy(
                    material = SidebarCardMaterial.entries[index % 3],
                    gradient = SidebarCardGradient.entries[index % 7],
                    start = 0xFF123456L + index,
                    imageUri = "file:///data/user/0/app/files/$index-$dark.image",
                    crop = CustomWallpaperCrop(.1f, .2f, .8f, .9f), inheritImage = false,
                ))
            }
        }
        val restored = decodeSidebarHomeCards(encodeSidebarHomeCards(cards))
        assertEquals(22, restored.overrides.size)
        assertEquals(cards, restored)
    }

    @Test
    fun changingOneCardDoesNotAffectTheOtherCardsOrNight() {
        val cards = SidebarHomeCards().withStyle(SidebarHomeCardId.Superuser, false,
            SidebarHomeCardStyle(material = SidebarCardMaterial.LiquidGlass, imageUri = "content://test/image"))
        assertEquals(SidebarCardMaterial.LiquidGlass, cards.style(SidebarHomeCardId.Superuser, false).material)
        for (id in SidebarHomeCardId.entries) {
            assertEquals(defaultSidebarHomeCardStyle(true), cards.style(id, true))
            if (id != SidebarHomeCardId.Superuser) assertEquals(defaultSidebarHomeCardStyle(false), cards.style(id, false))
        }
        assertTrue(cards.needsBackdrop(false))
        assertFalse(cards.needsBackdrop(true))
    }

    @Test
    fun bulkFinishPreservesEveryImageCropAndOtherLayout() {
        var cards = SidebarHomeCards()
        SidebarHomeCardId.entries.forEach { id ->
            cards = cards.withStyle(id, false, SidebarHomeCardStyle(imageUri = "content://test/${id.value}",
                inheritImage = false, crop = CustomWallpaperCrop(.2f, .1f, .9f, .8f)))
        }
        val finish = SidebarHomeCardStyle(material = SidebarCardMaterial.NeumorphicGlass, start = 0xFF332211,
            imageUri = "file:///should-not-be-copied", crop = FULL_CUSTOM_WALLPAPER_CROP)
        val updated = cards.applyFinish(SidebarHomeLayout.StatusCards, false, finish)
        for (id in SidebarHomeCardId.entries) {
            val before = cards.style(id, false)
            val after = updated.style(id, false)
            assertEquals(before.imageUri, after.imageUri)
            assertEquals(before.crop, after.crop)
            assertEquals(before.inheritImage, after.inheritImage)
            assertEquals(cards.style(id, true), updated.style(id, true))
            if (id in sidebarHomeCardIds(SidebarHomeLayout.StatusCards)) {
                assertEquals(finish.material, after.material)
                assertEquals(finish.start, after.start)
            } else assertEquals(before, after)
        }
    }

    @Test
    fun resetOnlyRemovesTheSelectedPair() {
        var cards = SidebarHomeCards()
        for (id in SidebarHomeCardId.entries) for (dark in listOf(false, true)) {
            cards = cards.withStyle(id, dark, defaultSidebarHomeCardStyle(dark).copy(material = SidebarCardMaterial.LiquidGlass))
        }
        val reset = cards.reset(SidebarHomeCardId.StatusHero, true)
        assertEquals(21, reset.overrides.size)
        for (id in SidebarHomeCardId.entries) for (dark in listOf(false, true)) {
            val expected = if (id == SidebarHomeCardId.StatusHero && dark) defaultSidebarHomeCardStyle(true) else cards.style(id, dark)
            assertEquals(expected, reset.style(id, dark))
        }
    }

    @Test
    fun malformedAndOversizedPayloadsUseSafeDefaults() {
        for (raw in listOf(null, "", "null", "[]", "broken", "{", " ".repeat(128 * 1024 + 1))) {
            assertEquals(SidebarHomeCards(), decodeSidebarHomeCards(raw))
        }
        assertEquals(SidebarHomeCards(), decodeSidebarHomeCards("""{"unknown.light":{}}"""))
        val restored = decodeSidebarHomeCards("""{"module.dark":{"material":"future","gradient":"future"}}""")
        assertEquals(defaultSidebarHomeCardStyle(true), restored.style(SidebarHomeCardId.Module, true))
    }

    @Test
    fun invalidColorsTintCropAndImageUrisAreNormalized() {
        val invalid = SidebarHomeCardStyle(start = -1, middle = 0x11FFFFFF, end = Long.MAX_VALUE,
            content = 0, highlight = 0xFFABCDEF, tint = Float.NaN, imageUri = "https://remote/image",
            crop = CustomWallpaperCrop(Float.NaN, -5f, Float.POSITIVE_INFINITY, 99f))
        for (dark in listOf(false, true)) {
            val style = invalid.normalized(dark)
            val expected = defaultSidebarHomeCardStyle(dark)
            assertEquals(expected.start, style.start)
            assertEquals(expected.middle, style.middle)
            assertEquals(expected.end, style.end)
            assertEquals(expected.content, style.content)
            assertEquals(expected.tint, style.tint)
            assertEquals(0xFFABCDEFL, style.highlight)
            assertEquals(FULL_CUSTOM_WALLPAPER_CROP, style.crop)
            assertNull(style.imageUri)
        }
        for (uri in listOf("", "null", "javascript:test", "file://" + "x".repeat(4096))) {
            assertNull(SidebarHomeCardStyle(imageUri = uri).normalized(false).imageUri)
        }
        assertEquals(0.15f, SidebarHomeCardStyle(tint = -1f).normalized(false).tint)
        assertEquals(1f, SidebarHomeCardStyle(tint = 2f).normalized(false).tint)
    }

    @Test
    fun unknownKeysAreDroppedAndNewKeyTriggersRefresh() {
        assertTrue(SidebarHomeCards(mapOf("unknown.dark" to SidebarHomeCardStyle())).normalized().overrides.isEmpty())
        assertTrue(isSidebarWidgetPreference(SIDEBAR_HOME_CARDS_KEY))
        assertEquals(SidebarHomeCards(), decodeSidebarHomeCards(encodeSidebarHomeCards(SidebarHomeCards())))
    }

    @Test
    fun layoutsCoverAllCardsAndOnlyShareSupportAndLearn() {
        val material = sidebarHomeCardIds(SidebarHomeLayout.Material)
        val status = sidebarHomeCardIds(SidebarHomeLayout.StatusCards)
        assertEquals(7, material.size)
        assertEquals(6, status.size)
        assertEquals(setOf(SidebarHomeCardId.Support, SidebarHomeCardId.Learn), material.toSet().intersect(status.toSet()))
        assertEquals(SidebarHomeCardId.entries.toSet(), (material + status).toSet())
    }

    @Test
    fun cardChangesDoNotAffectSidebarAvatarWeatherOrNavigation() {
        val config = SidebarWidgetConfig(side = SidebarSide.Right, imageUriString = "content://avatar",
            navigationOrder = SIDEBAR_NAVIGATION_IDS.reversed(), material = SidebarMaterial.Flat).normalized()
        val next = config.copy(homeCards = config.homeCards.withStyle(SidebarHomeCardId.Module, false,
            SidebarHomeCardStyle(material = SidebarCardMaterial.LiquidGlass))).normalized()
        assertEquals(config, next.copy(homeCards = config.homeCards))
    }
}
