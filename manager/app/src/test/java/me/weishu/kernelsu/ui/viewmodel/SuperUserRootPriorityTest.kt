package me.weishu.kernelsu.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuperUserRootPriorityTest {
    private data class App(
        val name: String,
        val installed: Long,
        val updated: Long = installed,
        val rank: Int = 3,
        val hasRoot: Boolean = rank == 0,
    )

    private fun displayOrder(apps: List<App>): List<App> = superUserDisplayOrder(
        apps,
        firstInstallTime = { it.installed },
        hasRoot = { it.hasRoot },
    )

    @Test
    fun rootAppsPrecedeTheNewestUnprivilegedAppInEverySortMode() {
        val apps = listOf(
            App("Root Z", 100, 200, rank = 0),
            App("Newest", 500, 600),
            App("Root A", 200, 300, rank = 0),
            App("Custom", 300, 400, rank = 1),
            App("Normal", 400, 500),
        )
        for (option in 0..7) {
            val base = when (option / 2) {
                SORT_BY_INSTALL_TIME -> compareBy<App> { it.installed }
                SORT_BY_UPDATE_TIME -> compareBy<App> { it.updated }
                else -> compareBy<App> { it.name }
            }
            val ordered = apps.sortedWith(superUserSortComparator(option, base) { it.rank })
            val result = displayOrder(ordered)
            val previousOrder = moveMostRecentlyInstalledFirst(ordered) { it.installed }

            assertEquals("Root priority for option $option", listOf(true, true, false, false, false), result.map { it.hasRoot })
            assertEquals(previousOrder.filter { it.hasRoot }, result.filter { it.hasRoot })
            assertEquals(previousOrder.filterNot { it.hasRoot }, result.filterNot { it.hasRoot })
        }
    }

    @Test
    fun newestRootAppRemainsFirstWithinTheAuthorizedGroup() {
        val apps = listOf(App("Older root", 100, rank = 0), App("Normal", 200), App("Newest root", 300, rank = 0))
        assertEquals(listOf("Newest root", "Older root", "Normal"), displayOrder(apps).map { it.name })
    }

    @Test
    fun rootPriorityIsReappliedWhenAuthorizationChangesWithoutResorting() {
        val apps = listOf(App("Existing root", 100, rank = 0), App("Newest", 400), App("Granted", 200), App("Normal", 300))
        val granted = apps.map { if (it.name == "Granted") it.copy(hasRoot = true) else it }
        val afterGrant = displayOrder(granted)
        assertEquals(listOf("Existing root", "Granted", "Newest", "Normal"), afterGrant.map { it.name })

        val revoked = afterGrant.map { if (it.name == "Existing root") it.copy(hasRoot = false) else it }
        assertEquals(listOf("Granted", "Newest", "Existing root", "Normal"), displayOrder(revoked).map { it.name })
    }

    @Test
    fun sharedUidGroupUsesAnyAuthorizedMemberNotOnlyThePrimaryApp() {
        val primary = App("Primary", 100)
        val authorizedMember = App("Authorized member", 200, rank = 0)
        val shared = listOf(primary, authorizedMember)
        val newestUnprivileged = listOf(App("Newest", 300))
        val result = superUserDisplayOrder(
            listOf(newestUnprivileged, shared),
            firstInstallTime = { group -> group.maxOf { it.installed } },
            hasRoot = { group -> group.any { it.hasRoot } },
        )
        assertEquals(listOf(shared, newestUnprivileged), result)
    }

    @Test
    fun customProfilesWithoutRootDoNotJoinTheAuthorizedGroup() {
        val apps = listOf(App("Custom", 300, rank = 1), App("Root", 100, rank = 0), App("Normal", 200))
        assertEquals(listOf("Root", "Custom", "Normal"), displayOrder(apps).map { it.name })
    }

    @Test
    fun filteringTheDisplayListPreservesRootPriority() {
        val apps = listOf(App("Match normal", 300), App("Other", 200), App("Match root", 100, rank = 0))
        val filtered = displayOrder(apps).filter { it.name.startsWith("Match") }
        assertEquals(listOf("Match root", "Match normal"), filtered.map { it.name })
    }

    @Test
    fun equalInstallationTimesPreserveTheOrderWithinEachGroup() {
        val apps = listOf(App("Normal A", 100), App("Root B", 100, rank = 0), App("Normal B", 100), App("Root A", 100, rank = 0))
        assertEquals(listOf("Root B", "Root A", "Normal A", "Normal B"), displayOrder(apps).map { it.name })
    }

    @Test
    fun emptyAndUniformAuthorizationListsRetainExistingPinningBehavior() {
        assertTrue(displayOrder(emptyList()).isEmpty())
        for (hasRoot in listOf(false, true)) {
            val single = listOf(App("Single", 100, hasRoot = hasRoot))
            assertEquals(single, displayOrder(single))
            val apps = listOf(App("Older", 100, hasRoot = hasRoot), App("Newest", 200, hasRoot = hasRoot))
            assertEquals(moveMostRecentlyInstalledFirst(apps) { it.installed }, displayOrder(apps))
        }
    }

    @Test
    fun repeatedOrderingDoesNotMutateDuplicateOrLoseApps() {
        val apps = listOf(App("Normal A", Long.MIN_VALUE), App("Root", 0, rank = 0), App("Newest", Long.MAX_VALUE), App("Normal B", 100))
        val original = apps.toList()
        val result = displayOrder(apps)
        assertEquals(original, apps)
        assertEquals(apps.toSet(), result.toSet())
        assertEquals(apps.size, result.size)
        assertEquals(result, displayOrder(result))
    }
}
