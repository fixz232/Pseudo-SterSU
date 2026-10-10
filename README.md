<div align="center">
  <h1>SterSU</h1>
  <p>基于 KernelSU 的 Android 内核 Root 管理器</p>
  <p>
    <strong>简体中文</strong> ·
    <a href="README.en.md">English</a> ·
    <a href="README.fr.md">Français</a> ·
    <a href="README.ru.md">Русский</a> ·
    <a href="README.ja.md">日本語</a> ·
    <a href="README.ko.md">한국어</a> ·
    <a href="README.es.md">Español</a>
  </p>
  <p><a href="https://t.me/+LkrMQKXtXvpmYmNl">Telegram</a></p>
</div>

SterSU 是基于 [KernelSU](https://github.com/tiann/KernelSU) 的开源衍生项目，面向 GKI 与 LKM 场景。它保留 Root 授权和模块管理能力，并扩展内核维护、管理器界面及可选功能。部分实现参考 [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) 等项目；SterSU 不是 KernelSU 或 SukiSU-Ultra 的官方发行版。

## 功能概览

- **Root 与模块管理**：管理应用授权、模块安装和启停。
- **GKI / LKM 维护**：关注 KMI 匹配、内核安装与镜像修补；具体可用能力取决于设备和内核构建。
- **动态管理器**：允许一个已安装的兼容应用获得副管理器权限。该权限等同于完整 Root 管理权限，启用前请阅读[安全与使用说明](./docs/DYNAMIC_MANAGER.md)。
- **GKI KPM**：在 AArch64 GKI 且启用 `CONFIG_KSU=y`、`CONFIG_KPM=y` 的内核上提供兼容接口；LKM 使用独立的 KPatch-Next 后端。详见 [KPM 来源与兼容性声明](./docs/SUKISU_KPM_NOTICE.md)。
- **ABK Control**：在启用 `CONFIG_ABK_CONTROL` 时提供兼容桥，并校验管理器的包名、证书大小和 SHA-256。详见 [ABK Control 说明](./docs/ABK_CONTROL.md)。
- **界面扩展**：提供多种管理器界面风格及可选商店功能。

## 侧栏天气

天气功能需在设置中阅读声明并确认启用；选择数据源本身不会立即查询。不同数据源的接口和数据处理方式如下：

- **小米天气**：读取设备本机天气提供方的数据，不申请定位权限；设备未提供接口时显示不可用。详见 [小米天气接口说明](./docs/XIAOMI_WEATHER_PROVIDER.md)。
- **Open-Meteo**：使用用户填写的经纬度请求第三方天气服务，服务端可见网络 IP；显示名称仅保存在本机。使用条件与署名见 [Open-Meteo 接口说明](./docs/OPEN_METEO_SIDEBAR.md)。

## 许可与来源

SterSU 继承上游的许可边界，具体以各文件声明为准：

- `kernel/` 目录遵循 **GPL-2.0-only**，除非单个文件另有声明。
- `kernel/` 以外的 KernelSU 衍生代码遵循 **GPL-3.0-or-later**。
- 第三方文件保留各自的许可证与版权声明，详见 [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md) 和 [NOTICE](./NOTICE)。

本仓库 [Pseudo-SterSU](https://github.com/fixz232/Pseudo-SterSU) 是 SterSU 的备用源码发布位置，不构成重新授权。根目录 [LICENSE](./LICENSE) 提供 GPL 第 3 版文本，不改变 `kernel/` 的许可。分发构建产物时，应遵守适用许可证、保留来源与声明，并提供完整对应源码。更多说明见 [GPL 合规说明](./GPL-COMPLIANCE.md)。

界面设计参考了开源项目，但 SterSU 的 MIUI 风格不是小米官方产品，也未使用小米官方源码。侧栏相关设计来源见 [Aster UI 说明](./docs/ASTER_UI_DESIGN_NOTICE.md)。

## 使用须知

- 修改内核、刷入镜像或安装模块可能导致无法开机、数据丢失或设备损坏。操作前请核对设备与内核版本并做好备份；项目不提供维修、赔偿或售后服务。
- 金融、游戏、企业及政务应用可能限制 Root 设备的使用。SterSU 不保证绕过检测，也不提供账号申诉或风控解除服务。
- 请仅在自己拥有或获授权的设备上合法使用；不得用于未授权的权限篡改、破解、数据窃取、恶意捆绑或作弊。
- 项目无官方付费销售或定制服务。第三方安装包和修改版的来源、完整性及安全性需自行核验。
- 使用前请阅读本页、适用许可证和相关功能文档；如不接受上述风险，请勿安装或刷入。

## 致谢

- [KernelSU](https://github.com/tiann/KernelSU)：主上游项目，感谢 weishu 与所有贡献者。
- [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) 与 [susfs4ksu](https://gitlab.com/simonpunk/susfs4ksu)：SuSFS 集成与补丁来源。
- [ReSukiSU](https://github.com/ReSukiSU/ReSukiSU)：动态管理器设计与接口参考。
- [FolkPatch](https://github.com/LyraVoid/FolkPatch)、[Aster](https://github.com/LyraVoid/Aster) 与 [SKRoot](https://github.com/abcz316/SKRoot-linuxKernelRoot)：界面代码与设计参考。
- [KOWX712/KernelSU](https://github.com/KOWX712/KernelSU)：内核模块文件来源。
- [Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/) 与 [Magisk](https://github.com/topjohnwu/Magisk)：技术思路参考。
- [genuine](https://github.com/brevent/genuine/) 与 [Diamorphine](https://github.com/m0nad/Diamorphine)：签名校验与底层实现参考。
