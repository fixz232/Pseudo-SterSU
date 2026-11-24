package me.weishu.kernelsu.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SuperUserSortOrderTest {
    private data class App(val name: String, val timestamp: Long, val rank: Int)

    @Test
    fun newDefaultIsNewestInstallationFirst() {
        assertEquals(5, DEFAULT_SUPERUSER_SORT_OPTION)
        assertEquals(5, resolveSuperUserSortOption(null, recentDefaultApplied = false))
    }

    @Test
    fun legacySortMigratesToRecentFirstOnlyOnce() {
        assertEquals(5, resolveSuperUserSortOption(0, recentDefaultApplied = false))
        assertEquals(0, resolveSuperUserSortOption(0, recentDefaultApplied = true))
        assertEquals(6, resolveSuperUserSortOption(6, recentDefaultApplied = true))
    }

    @Test
    fun invalidOrMissingSavedOrderUsesTheDefault() {
        for (option in listOf(null, -1, 8, Int.MAX_VALUE)) {
            assertEquals(5, resolveSuperUserSortOption(option, recentDefaultApplied = true))
        }
    }

    @Test
    fun installationComparatorUsesTimeBeforeFinalRootPrioritization() {
        val apps = listOf(App("Root", 100, 0), App("Recent", 300, 3), App("Custom", 200, 1))
        val order = superUserSortComparator(DEFAULT_SUPERUSER_SORT_OPTION, compareBy<App> { it.timestamp }) { it.rank }
        assertEquals(listOf("Recent", "Custom", "Root"), apps.sortedWith(order).map { it.name })
    }

    @Test
    fun reversingInstallationOrderShowsOldestFirst() {
        val apps = listOf(App("Recent", 300, 0), App("Old", 100, 3), App("Middle", 200, 1))
        val order = superUserSortComparator(SORT_BY_INSTALL_TIME * 2, compareBy<App> { it.timestamp }) { it.rank }
        assertEquals(listOf("Old", "Middle", "Recent"), apps.sortedWith(order).map { it.name })
    }

    @Test
    fun updateComparatorUsesTimeBeforeFinalRootPrioritization() {
        val apps = listOf(App("Root", 100, 0), App("Updated", 200, 3))
        val order = superUserSortComparator(SORT_BY_UPDATE_TIME * 2 + 1, compareBy<App> { it.timestamp }) { it.rank }
        assertEquals(listOf("Updated", "Root"), apps.sortedWith(order).map { it.name })
    }

    @Test
    fun equalTimesKeepAuthorizationAsATieBreaker() {
        val apps = listOf(App("Normal", 100, 3), App("Root", 100, 0))
        val order = superUserSortComparator(DEFAULT_SUPERUSER_SORT_OPTION, compareBy<App> { it.timestamp }) { it.rank }
        assertEquals(listOf("Root", "Normal"), apps.sortedWith(order).map { it.name })
    }

    @Test
    fun nameOrderStillGroupsRootAndCustomProfilesFirst() {
        val apps = listOf(App("A", 100, 3), App("Z", 100, 0), App("B", 100, 3))
        val order = superUserSortComparator(SORT_BY_NAME * 2, compareBy<App> { it.name }) { it.rank }
        assertEquals(listOf("Z", "A", "B"), apps.sortedWith(order).map { it.name })
    }

    @Test
    fun reverseNameOrderStillPreservesAuthorizationGroups() {
        val apps = listOf(App("A", 100, 3), App("Z", 100, 0), App("B", 100, 3))
        val order = superUserSortComparator(SORT_BY_NAME * 2 + 1, compareBy<App> { it.name }) { it.rank }
        assertEquals(listOf("Z", "B", "A"), apps.sortedWith(order).map { it.name })
    }

    @Test
    fun recentOrderWorksForEmptyAndSingleItemLists() {
        val order = superUserSortComparator(DEFAULT_SUPERUSER_SORT_OPTION, compareBy<App> { it.timestamp }) { it.rank }
        assertEquals(emptyList<App>(), emptyList<App>().sortedWith(order))
        val single = listOf(App("Single", 100, 0))
        assertEquals(single, single.sortedWith(order))
    }

    @Test
    fun onlyNewestAppMovesToTheFrontAndTheInputIsUnchanged() {
        val apps = listOf(App("Root", 200, 0), App("A", 100, 3), App("Newest", 400, 3), App("B", 300, 3))
        val result = moveMostRecentlyInstalledFirst(apps) { it.timestamp }
        assertEquals(listOf("Newest", "Root", "A", "B"), result.map { it.name })
        assertEquals(listOf("Root", "A", "Newest", "B"), apps.map { it.name })
    }

    @Test
    fun newestAppAlreadyFirstKeepsTheOriginalList() {
        val apps = listOf(App("Newest", 400, 3), App("Root", 100, 0), App("A", 200, 3))
        assertSame(apps, moveMostRecentlyInstalledFirst(apps) { it.timestamp })
    }

    @Test
    fun equalInstallationTimesPromoteOnlyTheFirstMatchingApp() {
        val apps = listOf(App("Root", 100, 0), App("First", 400, 3), App("A", 200, 3), App("Second", 400, 3))
        val result = moveMostRecentlyInstalledFirst(apps) { it.timestamp }
        assertEquals(listOf("First", "Root", "A", "Second"), result.map { it.name })
    }

    @Test
    fun equalTimesThroughoutKeepTheOriginalList() {
        val apps = listOf(App("A", 100, 3), App("B", 100, 0), App("C", 100, 3))
        assertSame(apps, moveMostRecentlyInstalledFirst(apps) { it.timestamp })
    }

    @Test
    fun pinningSupportsEmptyAndSingleItemLists() {
        val empty = emptyList<App>()
        assertSame(empty, moveMostRecentlyInstalledFirst(empty) { it.timestamp })
        val single = listOf(App("Single", 100, 0))
        assertSame(single, moveMostRecentlyInstalledFirst(single) { it.timestamp })
    }

    @Test
    fun newestMemberOfSharedUidGroupDeterminesThePinnedGroup() {
        val older = listOf(App("Root", 100, 0))
        val shared = listOf(App("Primary", 50, 0), App("NewMember", 300, 3))
        val other = listOf(App("Other", 200, 3))
        val groups = listOf(older, shared, other)
        assertEquals(listOf(shared, older, other), moveMostRecentlyInstalledFirst(groups) { group -> group.maxOf { it.timestamp } })
    }

    @Test
    fun pinningDoesNotDependOnTheCurrentTimeOrTimestampSubtraction() {
        val apps = listOf(App("Old", Long.MIN_VALUE, 0), App("Newest", Long.MAX_VALUE, 3), App("Middle", 0, 3))
        assertEquals(listOf("Newest", "Old", "Middle"), moveMostRecentlyInstalledFirst(apps) { it.timestamp }.map { it.name })
    }

    @Test
    fun repeatedPinningDoesNotDuplicateOrReorderOtherApps() {
        val apps = listOf(App("A", 100, 3), App("B", 400, 0), App("C", 200, 3))
        val pinned = moveMostRecentlyInstalledFirst(apps) { it.timestamp }
        assertSame(pinned, moveMostRecentlyInstalledFirst(pinned) { it.timestamp })
        assertEquals(listOf("B", "A", "C"), pinned.map { it.name })
    }
}
