package me.weishu.kernelsu.ui.screen.superuser

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuperUserLayoutContractTest {
    private fun source(path: String): String = File("src/main/java/me/weishu/kernelsu/$path").readText().replace("\r\n", "\n")
    private val miuix = source("ui/screen/superuser/SuperUserMiuix.kt")
    private val sidebar = source("ui/screen/superuser/SuperUserMaterial.kt")

    @Test
    fun bothHeadersUseTheSameOverflowMenuAndKeepTheirActions() {
        for (screen in listOf(miuix, sidebar)) {
            assertTrue(screen.contains("SuperUserOverflowMenu("))
        }
        val menu = source("ui/screen/superuser/SuperUserOverflowMenu.kt")
        for (action in listOf(
            "R.string.superuser_sort_menu", "superUserSortLabel(uiState.sortOption)",
            "actions.onOpenSulog()", "actions.onOpenAppTools()",
            "actions.onToggleShowSystemApps()", "actions.onToggleShowOnlyPrimaryUserApps()",
        )) assertTrue(action, menu.contains(action))
        assertFalse(miuix.contains("OverlayListPopup("))
        assertFalse(sidebar.contains("DropdownMenuGroup("))
        assertTrue(sidebar.contains("showTools = sidebar"))
        assertTrue(miuix.contains("showTools = true"))
    }

    @Test
    fun bothPagesShareOneFrostedRepositoryStyleGroup() {
        val surface = source("ui/component/overflow/FrostedOverflowMenu.kt")
        val module = source("ui/screen/module/ModuleOverflowMenu.kt")
        val menu = source("ui/screen/superuser/SuperUserOverflowMenu.kt")
        assertTrue(module.contains("FrostedOverflowMenu("))
        assertTrue(menu.contains("FrostedOverflowMenu("))
        assertTrue(Regex("\\bDropdownMenuPopup\\(").findAll(surface).count() == 1)
        assertTrue(Regex("\\bDropdownMenuGroup\\(").findAll(surface).count() == 1)
        assertTrue(surface.contains("shapes = MenuDefaults.groupShapes()"))
        assertTrue(surface.contains("containerColor = Color.Transparent"))
        assertTrue(surface.contains("tonalElevation = 0.dp"))
        assertFalse(surface.contains("Surface("))
    }

    @Test
    fun sortingChangesTheSamePopupContentAndResetsWhenDismissed() {
        val menu = source("ui/screen/superuser/SuperUserOverflowMenu.kt")
        assertTrue(menu.contains("if (showSortMenu) {"))
        assertTrue(menu.contains("Icons.AutoMirrored.Filled.ArrowBack"))
        assertTrue(menu.contains("onClick = { showSortMenu = false }"))
        assertFalse(menu.contains("expanded = showSortMenu"))
        assertTrue(menu.contains("LaunchedEffect(expanded, showSortMenu)"))
        assertTrue(menu.contains("if (expanded) menuScrollState.scrollTo(0) else showSortMenu = false"))
        assertTrue(menu.contains("FrostedOverflowMenu(expanded, onDismissRequest, wallpaper, menuScrollState)"))
    }

    @Test
    fun checkableStatesAndSmallWindowBoundsArePreserved() {
        val menu = source("ui/screen/superuser/SuperUserOverflowMenu.kt")
        val surface = source("ui/component/overflow/FrostedOverflowMenu.kt")
        assertTrue(surface.contains("overflowMenuWidth(window.widthDp, window.fontScale).dp"))
        assertTrue(surface.contains("heightIn(max = overflowMenuMaxHeight(window.heightDp).dp)"))
        assertTrue(surface.contains("verticalScroll(scrollState)"))
        for (state in listOf("reversed", "uiState.showSystemApps", "uiState.showOnlyPrimaryUserApps")) {
            assertTrue(menu.contains(", $state,"))
        }
        assertTrue(menu.contains("toggleableState = ToggleableState(checked)"))
        assertTrue(menu.contains("selected = sortType == type"))
        assertTrue(menu.contains("maxLines = 2, overflow = TextOverflow.Ellipsis"))
    }

    @Test
    fun overflowRowsUseTheSameSelectableComponentAsTheRepository() {
        val surface = source("ui/component/overflow/FrostedOverflowMenu.kt")
        val repository = source("ui/screen/modulerepo/ModuleRepoMaterial.kt")
        for (menu in listOf(surface, repository)) {
            assertTrue(menu.contains("SelectableDropdownMenuItem("))
            assertTrue(menu.contains("shapes = MenuDefaults.itemShape("))
            assertTrue(menu.contains("selectedLeadingIcon = {"))
            assertTrue(menu.contains("MenuDefaults.LeadingIconSize"))
        }
        assertTrue(surface.contains(".width(IntrinsicSize.Max)"))
        assertTrue(surface.contains(".padding(MenuDefaults.DropdownMenuGroupContentPadding)"))
        assertFalse(surface.contains("fontSize = 17.sp"))
        assertFalse(surface.contains(".heightIn(min = 56.dp)"))
        assertTrue(source("ui/screen/module/ModuleOverflowMenu.kt").contains("CompactOverflowMenuItem("))
        assertTrue(source("ui/screen/superuser/SuperUserOverflowMenu.kt").contains("CompactOverflowMenuItem("))
    }

    @Test
    fun titleSearchAndListKeepTopSpacingInBothMiuixLayouts() {
        assertTrue(miuix.contains("modifier = Modifier.padding(top = 8.dp)"))
        assertTrue(miuix.contains("8.dp + 4.dp * (1f - scrollBehavior.state.collapsedFraction)"))
        assertTrue(miuix.contains("top = innerPadding.calculateTopPadding() + 12.dp"))
        assertTrue(sidebar.contains("topContentPadding = if (sidebar) 8.dp else 0.dp"))
        assertTrue(sidebar.contains("top = if (sidebar) 12.dp else 0.dp"))
    }

    @Test
    fun miuixAppRowsWrapStatusBadgesInsteadOfStackingThemAtTheRight() {
        val item = miuix.substringAfter("private fun GroupItem(").substringBefore("private fun SuperUserStatusBadge(")
        assertTrue(item.contains("FlowRow("))
        assertTrue(item.contains("maxLines = 2"))
        assertTrue(item.contains("TextOverflow.Ellipsis"))
        assertTrue(item.contains(".size(40.dp)"))
        assertFalse(item.contains("basicMarquee"))
        assertFalse(item.contains("horizontalAlignment = Alignment.End"))
    }

    @Test
    fun recentOrderUsesTheWholeUidGroupAndPersistsLaterChoices() {
        val viewModel = source("ui/viewmodel/SuperUserViewModel.kt")
        assertTrue(viewModel.contains("group.apps.maxOfOrNull { it.packageInfo.firstInstallTime }"))
        assertTrue(viewModel.contains("group.apps.maxOfOrNull { it.packageInfo.lastUpdateTime }"))
        assertTrue(viewModel.contains("superUserSortComparator(sortOption, base, ::groupRank)"))
        assertTrue(viewModel.contains("if (!recentDefaultApplied)"))
        assertTrue(viewModel.contains("putBoolean(PREFS_RECENT_SORT_DEFAULT, true)"))
    }

    @Test
    fun latestVisibleGroupIsPinnedAfterExistingSortingWithoutChangingPreferences() {
        val viewModel = source("ui/viewmodel/SuperUserViewModel.kt")
        val update = viewModel.substringAfter("private suspend fun updateVisibleApps(").substringBefore("private fun filterAndSort(")
        assertTrue(update.contains("val ordered = if (resort) sortGroups(grouped, sortOption) else grouped"))
        assertTrue(update.contains("moveMostRecentlyInstalledFirst(ordered)"))
        assertTrue(update.contains("group.apps.maxOfOrNull { it.packageInfo.firstInstallTime }"))
        assertTrue(update.contains("filterSearchResults(s, searchText)"))
        assertFalse(update.contains("putInt("))
        assertFalse(update.contains("RECENTLY_INSTALLED_WINDOW_MILLIS"))
    }
}
