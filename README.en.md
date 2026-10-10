<div align="center">
  <h1>SterSU</h1>
  <p>An Android kernel-based root manager derived from KernelSU</p>
  <p>
    <a href="README.md">简体中文</a> ·
    <strong>English</strong> ·
    <a href="README.fr.md">Français</a> ·
    <a href="README.ru.md">Русский</a> ·
    <a href="README.ja.md">日本語</a> ·
    <a href="README.ko.md">한국어</a> ·
    <a href="README.es.md">Español</a>
  </p>
  <p><a href="https://t.me/+LkrMQKXtXvpmYmNl">Telegram</a></p>
</div>

SterSU is an open-source derivative of [KernelSU](https://github.com/tiann/KernelSU) for GKI and LKM scenarios. It retains root authorization and module management while extending kernel maintenance, the Manager UI, and optional features. Some implementations draw on [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) and other projects; SterSU is not an official KernelSU or SukiSU-Ultra release.

## Features

- **Root and modules**: manage app permissions and install, enable, or disable modules.
- **GKI / LKM maintenance**: focus on KMI matching, kernel installation, and image patching. Available capabilities depend on the device and kernel build.
- **Dynamic Manager**: grant one installed compatible app secondary Manager authority. This is full root-management authority; read the [security and usage guide](./docs/DYNAMIC_MANAGER.md) before enabling it.
- **GKI KPM**: provide a compatible interface on AArch64 GKI kernels built with `CONFIG_KSU=y` and `CONFIG_KPM=y`. LKM uses a separate KPatch-Next backend. See the [KPM origin and compatibility notice](./docs/SUKISU_KPM_NOTICE.md).
- **ABK Control**: provide a compatibility bridge when `CONFIG_ABK_CONTROL` is enabled, checking the Manager package name, certificate size, and SHA-256. See the [ABK Control guide](./docs/ABK_CONTROL.md).
- **UI extensions**: offer multiple Manager interface styles and optional store features.

## Sidebar weather

To use weather data, read and accept the disclosure in Settings. Merely selecting a provider does not start a query. The providers handle data differently:

- **Xiaomi Weather**: reads the device's local weather provider without requesting location permission. If the provider is unavailable, the widget shows an unavailable state. See the [Xiaomi Weather interface notice](./docs/XIAOMI_WEATHER_PROVIDER.md).
- **Open-Meteo**: sends user-entered coordinates to a third-party weather service, which can also see the network IP address. The optional display name remains on the device. See the [Open-Meteo interface notice](./docs/OPEN_METEO_SIDEBAR.md) for terms and attribution.

## Licensing and sources

SterSU retains the upstream licensing boundaries. File-level notices take precedence:

- `kernel/` is **GPL-2.0-only** unless an individual file states otherwise.
- KernelSU-derived code outside `kernel/` is **GPL-3.0-or-later**.
- Third-party files retain their respective licenses and copyright notices; see [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md) and [NOTICE](./NOTICE).

[Pseudo-SterSU](https://github.com/fixz232/Pseudo-SterSU) is a backup publication of SterSU source, not a relicensed edition. The root [LICENSE](./LICENSE) contains the GPL version 3 text and does not change the license of `kernel/`. When distributing builds, comply with applicable licenses, preserve attribution and notices, and provide complete corresponding source. See the [GPL compliance notes](./GPL-COMPLIANCE.md).

The UI draws on open-source designs. SterSU's MIUI-style interface is not an official Xiaomi product and does not use Xiaomi source code. See the [Aster UI attribution](./docs/ASTER_UI_DESIGN_NOTICE.md) for sidebar design references.

## Before use

- Changing a kernel, flashing an image, or installing a module may cause boot failure, data loss, or device damage. Check device and kernel compatibility and make a backup first. The project does not provide repair, compensation, or after-sales service.
- Financial, gaming, enterprise, and government apps may restrict rooted devices. SterSU does not guarantee detection bypass and does not assist with account appeals or risk-control removal.
- Use the project lawfully only on devices you own or are authorized to manage. Do not use it for unauthorized privilege changes, cracking, data theft, malicious bundling, or cheating.
- The project has no official paid sales or customization service. Check the provenance, integrity, and security of third-party APKs and modified builds yourself.
- Read this page, the applicable licenses, and feature documentation before use. Do not install or flash if you do not accept these risks.

## Acknowledgements

- [KernelSU](https://github.com/tiann/KernelSU): primary upstream project; thanks to weishu and all contributors.
- [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) and [susfs4ksu](https://gitlab.com/simonpunk/susfs4ksu): SuSFS integration and patch sources.
- [ReSukiSU](https://github.com/ReSukiSU/ReSukiSU): Dynamic Manager design and interface reference.
- [FolkPatch](https://github.com/LyraVoid/FolkPatch), [Aster](https://github.com/LyraVoid/Aster), and [SKRoot](https://github.com/abcz316/SKRoot-linuxKernelRoot): UI code and design references.
- [KOWX712/KernelSU](https://github.com/KOWX712/KernelSU): kernel module file source.
- [Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/) and [Magisk](https://github.com/topjohnwu/Magisk): technical inspiration.
- [genuine](https://github.com/brevent/genuine/) and [Diamorphine](https://github.com/m0nad/Diamorphine): signature-checking and low-level implementation references.
