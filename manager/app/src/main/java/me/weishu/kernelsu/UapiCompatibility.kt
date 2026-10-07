package me.weishu.kernelsu

/** UAPI 5 only adds the optional services event; UAPI 4 profiles remain compatible. */
internal fun isCompatibleUapiVersion(kernelVersion: Int, managerVersion: Int): Boolean =
    kernelVersion == managerVersion || (kernelVersion == 4 && managerVersion == 5)
