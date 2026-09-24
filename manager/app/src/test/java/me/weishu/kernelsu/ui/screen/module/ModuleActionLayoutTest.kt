package me.weishu.kernelsu.ui.screen.module

import me.weishu.kernelsu.data.model.Module
import org.junit.Assert.assertEquals
import org.junit.Test

class ModuleActionLayoutTest {
    @Test
    fun `undo uninstall has highest priority`() {
        val module = module(remove = true, hasWebUi = true, hasActionScript = true)

        assertEquals(
            ModulePrimaryAction.UndoUninstall,
            resolveModulePrimaryAction(module, hasUpdate = true),
        )
    }

    @Test
    fun `update is preferred over module entry points`() {
        val module = module(hasWebUi = true, hasActionScript = true)

        assertEquals(
            ModulePrimaryAction.Update,
            resolveModulePrimaryAction(module, hasUpdate = true),
        )
    }

    @Test
    fun `web ui is preferred over action`() {
        val module = module(hasWebUi = true, hasActionScript = true)

        assertEquals(
            ModulePrimaryAction.WebUi,
            resolveModulePrimaryAction(module, hasUpdate = false),
        )
    }

    @Test
    fun `action is primary when it is the only entry point`() {
        val module = module(hasActionScript = true)

        assertEquals(
            ModulePrimaryAction.Action,
            resolveModulePrimaryAction(module, hasUpdate = false),
        )
    }

    @Test
    fun `pending or disabled module has no primary action`() {
        assertEquals(
            ModulePrimaryAction.None,
            resolveModulePrimaryAction(module(update = true, hasWebUi = true), hasUpdate = false),
        )
        assertEquals(
            ModulePrimaryAction.None,
            resolveModulePrimaryAction(module(update = true, hasWebUi = true), hasUpdate = true),
        )
        assertEquals(
            ModulePrimaryAction.None,
            resolveModulePrimaryAction(module(enabled = false, hasWebUi = true), hasUpdate = false),
        )
    }

    private fun module(
        enabled: Boolean = true,
        update: Boolean = false,
        remove: Boolean = false,
        hasWebUi: Boolean = false,
        hasActionScript: Boolean = false,
    ) = Module(
        id = "test",
        name = "Test",
        author = "Tester",
        version = "1",
        versionCode = 1,
        description = "",
        enabled = enabled,
        update = update,
        remove = remove,
        updateJson = "",
        hasWebUi = hasWebUi,
        hasActionScript = hasActionScript,
        metamodule = false,
        actionIconPath = null,
        webUiIconPath = null,
    )
}
