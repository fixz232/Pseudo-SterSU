package me.weishu.kernelsu.ui.viewmodel

internal const val SORT_BY_NAME = 0
internal const val SORT_BY_PACKAGE_NAME = 1
internal const val SORT_BY_INSTALL_TIME = 2
internal const val SORT_BY_UPDATE_TIME = 3
internal const val DEFAULT_SUPERUSER_SORT_OPTION = SORT_BY_INSTALL_TIME * 2 + 1

internal fun resolveSuperUserSortOption(savedOption: Int?, recentDefaultApplied: Boolean): Int =
    if (recentDefaultApplied && savedOption != null && savedOption in 0..7) {
        savedOption
    } else {
        DEFAULT_SUPERUSER_SORT_OPTION
    }

internal fun <T> superUserSortComparator(
    sortOption: Int,
    base: Comparator<T>,
    stateRank: (T) -> Int,
): Comparator<T> {
    val order = if (sortOption % 2 != 0) base.reversed() else base
    return when (sortOption / 2) {
        SORT_BY_INSTALL_TIME, SORT_BY_UPDATE_TIME -> order.thenBy(stateRank)
        else -> compareBy(stateRank).then(order)
    }
}

internal fun <T> moveMostRecentlyInstalledFirst(
    items: List<T>,
    firstInstallTime: (T) -> Long,
): List<T> {
    val newestIndex = items.indices.maxByOrNull { firstInstallTime(items[it]) } ?: return items
    if (newestIndex == 0) return items
    return items.toMutableList().apply {
        add(0, removeAt(newestIndex))
    }
}

internal fun <T> superUserDisplayOrder(
    items: List<T>,
    firstInstallTime: (T) -> Long,
    hasRoot: (T) -> Boolean,
): List<T> {
    val newestFirst = moveMostRecentlyInstalledFirst(items, firstInstallTime)
    // Root access takes precedence over the newest-install pin and every sort mode.
    val (rootApps, otherApps) = newestFirst.partition(hasRoot)
    return if (rootApps.isEmpty() || otherApps.isEmpty()) newestFirst else rootApps + otherApps
}
