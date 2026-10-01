package me.weishu.kernelsu.ui.screen.module

import me.weishu.kernelsu.data.model.Module

internal enum class ModulePrimaryAction {
    UndoUninstall,
    Update,
    WebUi,
    Action,
    None,
}

internal fun resolveModulePrimaryAction(
    module: Module,
    hasUpdate: Boolean,
): ModulePrimaryAction = when {
    module.remove -> ModulePrimaryAction.UndoUninstall
    module.update -> ModulePrimaryAction.None
    hasUpdate -> ModulePrimaryAction.Update
    module.enabled && module.hasWebUi -> ModulePrimaryAction.WebUi
    module.enabled && module.hasActionScript -> ModulePrimaryAction.Action
    else -> ModulePrimaryAction.None
}
