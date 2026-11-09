ApkeSU third-party notices
==========================

This file records direct third-party dependencies and build-time source inputs
used by this repository. It complements LICENSE, NOTICE, and
GPL-COMPLIANCE.md.

ApkeSU is an open-source derivative of
[KernelSU](https://github.com/tiann/KernelSU), with changes focused on the
Manager experience, interface extensions, LKM patching, and personal-device
debugging. The project periodically incorporates upstream source updates,
security fixes, and feature work from KernelSU.

Keep the upstream license text and notices with any redistributed binary
artifact. For release audits, also verify transitive dependency licenses from
the committed lock files and published package metadata.

1. Project license split
------------------------

ApkeSU inherits KernelSU's license split:

  - kernel/ is GPL-2.0-only unless an individual file says otherwise.
  - non-kernel KernelSU-derived code is GPL-3.0-or-later unless an individual
    file says otherwise.
  - third-party dependencies remain under their own licenses.

Redistributions must preserve the upstream origin, applicable license texts,
and third-party notices. Modified public distributions, including released
kernels, APKs, and modules, must provide the complete corresponding source as
required by the applicable license.

2. NPM packages
---------------

Source files:

  - js/package.json

Project/package declarations:

| Area | Package | Version/range | Declared license | Notes |
| --- | --- | --- | --- | --- |
| Module WebUI library | kernelsu | 3.0.2 | Apache-2.0 | No runtime npm dependencies are declared in js/package.json. |

3. Gradle and Maven dependencies
--------------------------------

Source files:

  - manager/settings.gradle.kts
  - manager/gradle/libs.versions.toml
  - manager/app/build.gradle.kts

Repositories used by Gradle:

  - google()
  - mavenCentral()
  - https://jitpack.io

Direct Gradle plugins:

| Coordinate/plugin | Version | License | Source/project |
| --- | --- | --- | --- |
| com.android.application | 9.3.0 | Apache-2.0 | Android Gradle Plugin |
| org.jetbrains.kotlin.android | 2.4.10 | Apache-2.0 | Kotlin |
| org.jetbrains.kotlin.plugin.compose | 2.4.10 | Apache-2.0 | Kotlin Compose compiler plugin |
| org.jetbrains.kotlin.plugin.serialization | 2.4.10 | Apache-2.0 | Kotlin serialization compiler plugin |
| org.lsposed.lsplugin.apksign | 1.4 | Apache-2.0 | https://github.com/LSPosed/LSPlugin |

Direct Manager dependencies:

| Coordinate | Version | License | Source/project |
| --- | --- | --- | --- |
| androidx.activity:activity-compose | 1.13.0 | Apache-2.0 | AndroidX |
| androidx.compose:compose-bom | 2026.06.01 | Apache-2.0 | AndroidX Compose |
| androidx.compose.material:material-icons-extended | BOM managed | Apache-2.0 | AndroidX Compose |
| androidx.compose.material3:material3 | 1.5.0-alpha24 | Apache-2.0 | AndroidX Compose Material3 |
| androidx.compose.ui:ui | BOM managed | Apache-2.0 | AndroidX Compose |
| androidx.compose.ui:ui-tooling-preview | BOM managed | Apache-2.0 | AndroidX Compose |
| androidx.compose.ui:ui-test-manifest | BOM managed | Apache-2.0 | AndroidX Compose, debug only |
| androidx.compose.ui:ui-tooling | BOM managed | Apache-2.0 | AndroidX Compose, debug only |
| androidx.lifecycle:lifecycle-runtime-compose | 2.11.0 | Apache-2.0 | AndroidX Lifecycle |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.11.0 | Apache-2.0 | AndroidX Lifecycle |
| androidx.lifecycle:lifecycle-viewmodel-compose | 2.11.0 | Apache-2.0 | AndroidX Lifecycle |
| androidx.lifecycle:lifecycle-viewmodel-navigation3 | 2.11.0 | Apache-2.0 | AndroidX Lifecycle |
| androidx.navigation3:navigation3-runtime | 1.1.4 | Apache-2.0 | AndroidX Navigation 3 |
| androidx.navigationevent:navigationevent-compose | 1.1.2 | Apache-2.0 | AndroidX Navigation Event |
| androidx.webkit:webkit | 1.16.0 | Apache-2.0 | AndroidX WebKit |
| com.github.topjohnwu.libsu:core | 6.0.0 | Apache-2.0 | https://github.com/topjohnwu/libsu |
| com.github.topjohnwu.libsu:service | 6.0.0 | Apache-2.0 | https://github.com/topjohnwu/libsu |
| com.github.topjohnwu.libsu:io | 6.0.0 | Apache-2.0 | https://github.com/topjohnwu/libsu |
| dev.rikka.rikkax.parcelablelist:parcelablelist | 2.0.1 | MIT | https://github.com/RikkaApps/RikkaX |
| org.jetbrains.kotlinx:kotlinx-coroutines-core | 1.11.0 | Apache-2.0 | kotlinx.coroutines |
| org.commonmark:commonmark | 0.29.0 | BSD-2-Clause | commonmark-java |
| org.commonmark:commonmark-ext-gfm-tables | 0.29.0 | BSD-2-Clause | commonmark-java |
| org.commonmark:commonmark-ext-gfm-strikethrough | 0.29.0 | BSD-2-Clause | commonmark-java |
| org.commonmark:commonmark-ext-autolink | 0.29.0 | BSD-2-Clause | commonmark-java |
| org.commonmark:commonmark-ext-task-list-items | 0.29.0 | BSD-2-Clause | commonmark-java |
| org.lsposed.libcxx:libcxx | 29.0.14206865 | Apache-2.0 / LLVM notices | https://github.com/LSPosed/prefab-libcxx |
| org.lsposed.hiddenapibypass:hiddenapibypass | 6.1 | Apache-2.0 | https://github.com/LSPosed/AndroidHiddenApiBypass |
| top.yukonga.miuix.kmp:miuix-ui-android | 0.9.3 | Apache-2.0 | https://github.com/compose-miuix-ui/miuix |
| top.yukonga.miuix.kmp:miuix-icons-android | 0.9.3 | Apache-2.0 | https://github.com/compose-miuix-ui/miuix |
| top.yukonga.miuix.kmp:miuix-navigation3-ui-android | 0.9.3 | Apache-2.0 | https://github.com/compose-miuix-ui/miuix |
| top.yukonga.miuix.kmp:miuix-preference-android | 0.9.3 | Apache-2.0 | https://github.com/compose-miuix-ui/miuix |
| top.yukonga.miuix.kmp:miuix-blur-android | 0.9.3 | Apache-2.0 | https://github.com/compose-miuix-ui/miuix |
| com.squareup.okhttp3:okhttp-bom | 5.4.0 | Apache-2.0 | https://github.com/square/okhttp |
| com.squareup.okhttp3:okhttp | 5.4.0 | Apache-2.0 | https://github.com/square/okhttp |
| com.materialkolor:material-kolor | 5.0.0 | MIT | https://github.com/jordond/materialkolor |
| me.zhanghai.android.appiconloader:appiconloader | 1.5.0 | Apache-2.0 | https://github.com/zhanghai/AppIconLoader |

Gradle resolves additional transitive dependencies from the repositories above.
For binary releases, generate a dependency report from Gradle and preserve all
licenses/notices from resolved artifacts.

Suggested release check:

  cd manager
  ./gradlew :app:dependencies --configuration releaseRuntimeClasspath

4. LKM build inputs
-------------------

Source files:

  - .github/workflows/build-lkm.yml
  - .github/workflows/ddk-lkm.yml

Direct external build inputs:

| Input | Version/ref used by repo | License/source note |
| --- | --- | --- |
| ghcr.io/ylarod/ddk-min | `${kmi}-20260313` | Android DDK container used only to compile kernelsu.ko. Kernel headers and generated kernel build files follow the Android common kernel/Linux GPL-2.0-only model; LLVM/Clang and Android build tools keep their own upstream notices. Keep the container/upstream notices with published KO build logs where applicable. |
| `userspace/ksud/bin/aarch64/*_kernelsu.ko` | [KOWX712/KernelSU](https://github.com/KOWX712/KernelSU), `master` at `60e7841b5ce1d0dc5aafd4d4898cd5dccacb270f` (2026-07-21) | Precompiled KernelSU LKM source reference. The exact build commit is not embedded in the `.ko` metadata and must be replaced with the actual build ref when known. |
| GitHub Actions official actions | checkout, upload-artifact | Build-service actions used at CI time; not vendored into release artifacts. |

The repository does not vendor the DDK container. It is fetched by CI at build
time and remains governed by its upstream licenses.

The KOWX712 KernelSU source uses GPL-2.0-only for `kernel/` (see
`kernel/LICENSE`) and GPL-3.0-or-later for the remaining project code (see
`LICENSE`). Redistribution of the referenced `.ko` files is permitted under
those applicable GPL terms, provided that the corresponding license texts,
copyright notices, and complete corresponding source (or the GPL-required
source offer) are preserved with the redistributed artifact.

The `.ko` ELF metadata reports `license=GPL`, but does not contain a Git
commit. The commit above is the audited upstream `master` reference, not a
claim that it is the exact build commit; record the exact build ref before
publishing a release when it is available.

When publishing a KO package, include:

  - the exact ApkeSU source revision;
  - the exact KMI and DDK release;
  - the generated binary artifact and enough build instructions to reproduce it.

5. ReSukiSU Dynamic Manager reference
-------------------------------------

The ApkeSU Dynamic Manager implementation was designed with reference to the
Dynamic Manager feature, IOCTL command allocation, and loading approach in
[ReSukiSU/ReSukiSU](https://github.com/ReSukiSU/ReSukiSU), audited at commit
[`0b5efe9e0102c43ca5c41174d500f5a7080cd0c7`](https://github.com/ReSukiSU/ReSukiSU/commit/0b5efe9e0102c43ca5c41174d500f5a7080cd0c7)
(2026-08-31).

The port follows the ReSukiSU certificate-only ABI and loading semantics in these upstream areas:

  - `kernel/feature/dynamic_manager.c` and `.h`;
  - `userspace/ksud/src/android/dynamic_manager.rs`;
  - the Manager Dynamic Manager repository, model, use cases, ViewModel, and UI.

ReSukiSU applies GPL-2.0-only to its `kernel/` directory and GPL-3.0-or-later
to the remaining project code. ApkeSU retains the same applicable license split
for this port. The IOCTL operations, certificate-only `{size, hash}` state,
dynamic signature index `255`, manager enumeration, APK selection, and manual
certificate entry intentionally follow ReSukiSU. ApkeSU-specific integration
keeps its built-in primary Manager restricted to `io.github.fixz.apkesu`, uses
its existing supercall/ksud lifecycle, persists state atomically, migrates the
previous ApkeSU schema-v1 certificate, and hardens incomplete scans and APK ZIP
validation. No ReSukiSU fixed-manager certificates or Vivo package variant are
included. See `docs/DYNAMIC_MANAGER.md` for the security contract and
limitations.

6. SukiSU-compatible GKI KPM interface
---------------------------------------

SterSU's Native GKI KPM interface is an explicitly declared compatibility
implementation. Its ABI and GKI integration were designed with reference to
[SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra), audited at
commit
[`31255c460caefef75e98b3406dd40efcc00fad38`](https://github.com/SukiSU-Ultra/SukiSU-Ultra/commit/31255c460caefef75e98b3406dd40efcc00fad38).
It is not an official SukiSU-Ultra distribution and does not imply endorsement
by that project.

The early-boot runtime and image patcher are the unmodified `kpimg` and
`kptools` Manager assets from that same SukiSU-Ultra commit. The actual bundled
runtime version is 0.13.0. Artifact hashes, Git blob IDs and the GPL-2.0-or-later
license are recorded in `third_party/sukisu-runtime/`. Same-version source:
[SukiSU_KernelPatch_patch 0.13.0](https://github.com/SukiSU-Ultra/SukiSU_KernelPatch_patch/tree/762e6fb446f58020691b83509873268f6af634f2).
This is a source reference, not a verified reproducible-build claim.
The local `third_party/kernelpatch` 0.13.9 hardening is retained for experiments
but is not included in those upstream binary assets. The default build pins
the SukiSU pair; see `docs/SUKISU_KPM_NOTICE.md` for scope and compatibility.

The SterSU implementation is limited to an AArch64 built-in GKI bridge
(`CONFIG_KSU=y`, `CONFIG_KPM=y`) and the declared KPM UAPI. Its supercall
authorization, ksud state machine, import validation, rescue recovery,
logging, and Manager integration are SterSU-specific. LKM and late-load builds
do not use this backend; they use the separate KPatch-Next path. This notice
does not relicense any upstream source: SPDX headers, upstream license texts,
and the source-availability requirements remain binding.

7. SUSFS GKI source patches
---------------------------

The built-in GKI SUSFS build downloads source patches from
[simonpunk/susfs4ksu](https://gitlab.com/simonpunk/susfs4ksu). The build
copies `kernel_patches/fs/*` and `kernel_patches/include/linux/*` into the
downloaded Android common kernel tree and applies the matching
`50_add_susfs_in_gki-*.patch`. These files are build-time source inputs; they
are not stored as generated kernel artifacts in this repository.

Reproducible builds use the branch-specific commits recorded in
`.github/config/gki-commits.json`:

| SUSFS branch | Pinned commit |
| --- | --- |
| `gki-android12-5.10` | `f3b5aecf53ff8b3296603071b91383f6be6c7cbb` |
| `gki-android13-5.10` | `04a9d713106191ba98be680bd7ad9547ab1de964` |
| `gki-android13-5.15` | `e565931d19256fd821ada01b35263506e7c7a364` |
| `gki-android14-5.15` | `b3c7f19b71b5d46c541094906e46e7c3edfcecc1` |
| `gki-android14-6.1` | `273ae364c5b7c92ceb15634c9f075b6fc0501048` |
| `gki-android15-6.6` | `9d9464f191b2d846590ac4d1b52e123df135c401` |
| `gki-android16-6.12` | `c8f64e41e3dea2cd44754d7472d3cd0bc0b40784` |

The upstream repository carries the GNU GPL version 3 license text. Preserve
its copyright and license notices and provide the complete corresponding
source for distributed builds. Individual imported files do not currently
carry SPDX identifiers, so this notice records the source repository and exact
revisions without claiming that those files have been relicensed under
ApkeSU's project-wide license split.

SUSFS source patches are enabled only for built-in AArch64 GKI builds with
`CONFIG_KSU_SUSFS=y`. LKM builds continue to use ApkeSU's existing dynamic-hook
implementation. Android 17 / Linux 6.18 is intentionally excluded because the
upstream repository does not publish a matching GKI source patch.

The 2026-10-25 GKI SUSFS compatibility update also references
[SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra), main revision
`72eea67e2307cb9d257a4bbd8dd6e64f10724bc5` and built-in branch revision
`ce75122bec7423e0f1fa06e3e9a174085c5231ab`. The ABI implementation detection
is adapted from `873b5c06dacc2293b53f170f126b00cf2abf72bb`; Manager operation
handling references `2d2f3ed186977bccf885d15a118155e9c9f849d8` and
`242dbc4d86f926e43b5dadce710390506e4a12a7`. The direct-input callback guard
adapts the safe-mode fix from `5a2bb7e5813002ccaabe02fa864cfb2dde6b5109`
to SterSU's existing GKI source-hook implementation. SukiSU-Ultra publishes
GPL version 3 license text; this attribution does not replace file-level
kernel SPDX terms or the project's license split. See
`docs/SUKISU_GKI_SUSFS_SYNC.md` for applied changes and preserved boundaries.

8. Aster UI design reference
----------------------------

SterSU's sidebar and page-layout work references the UI design of
[Aster](https://github.com/LyraVoid/Aster), by LyraVoid and contributors,
at commit `86488f8b204853caadce370b5646f9b35e31d0ce` (2026-09-25).
The reviewed references include its README, `GlobalLayout.kt`, and
`AsterAppShell.kt`. This attribution covers design inspiration for separating
navigation from content and maintaining consistent page and settings structure.

This sidebar update does not import Aster source files, assets, dependencies,
or its root backend. SterSU adapts these design ideas to its existing Manager
implementation. It is an independent project, not an official Aster release;
no affiliation or endorsement is implied. Existing FolkPatch code attribution
and all other third-party notices remain unchanged.

Aster publishes a GPL version 3 license text. See its
[pinned upstream license](https://github.com/LyraVoid/Aster/blob/86488f8b204853caadce370b5646f9b35e31d0ce/LICENSE)
and the detailed [UI design attribution](./docs/ASTER_UI_DESIGN_NOTICE.md).
This notice does not replace or relicense any source file or override the
project's existing licenses and copyright notices.

9. Optional LKM SUSFS guard
---------------------------

`kernel/susfs/` is a separate, optional SUSFS loadable kernel module. The
immediate vendored source is `kernel/susfs` from
[KernelSU-Next/KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next)
at commit `30932946f0ec6a69c2ce1e41109fa9bbf18aa384` (2026-11-09).
The previous snapshot was `4c706b5938665866ebe16944a481a7b91e3396af`.
Its original integration commit
[`8f573af08333`](https://github.com/KernelSU-Next/KernelSU-Next/commit/8f573af08333)
names [inforcqb/susfs4ksu-lkm](https://github.com/inforcqb/susfs4ksu-lkm)
commit `faf696a050b1c64009a4898cae7bcc0c9079f1d3` as its source. The LKM
port, in turn, credits `simonpunk/susfs4ksu` and SukiSU-Ultra for the SUSFS
logic/hook points and supporting hook/symbol code, respectively.

The LKM port and its kernel sources declare GPL-2.0. The full license text is
in `kernel/LICENSE`; original source-file SPDX and copyright notices are kept.
The `KernelSU-Next` root GPL-3.0 license does not change those kernel-file
terms. Repository attributes normalize the line endings of `sus_path.c` to LF.
The full pinned LKM snapshot includes the newer open-redirect fixes that
supersede the previously backported `b7c197621` and `bbe5db8f4` changes.
It also includes the vendored `kallrecon` symbol-resolution library; its
GPL-2.0 license is retained at `kernel/susfs/kallrecon/LICENSE`, together
with the upstream source-file copyright and SPDX notices.
SterSU adds separate ksud loading,
policy persistence, Manager status UI and KMI build wiring; those parts follow
their own file-level licenses and do not replace the GKI SUSFS implementation.
See `kernel/susfs/README.stersu.md` for the build and runtime boundaries.

This attribution identifies upstream source; it does not imply affiliation or
endorsement. Distributors of the LKM must preserve applicable notices and
provide complete corresponding source, including modifications and build
scripts, for the exact distributed binary.

10. Waline theme comments client
--------------------------------

The theme-store comment system uses [Waline](https://github.com/walinejs/waline).
Official documentation: https://waline.js.org/.

The Manager bundles the unmodified `@waline/client` 3.16.1 JavaScript and CSS
under `manager/app/src/main/assets/theme-comments/vendor/`. These client
assets are MIT-licensed. The original notice identifies
`Copyright (C) 2021 - PRESENT by Mr.Hope<mister-hope@outlook.com>`; its complete
copyright and permission text is retained in that directory's `LICENSE`.
The client attribution remains available in the comment UI.

SterSU-specific code implements Android WebView hosting, per-theme discussion
identifiers, appearance and wallpaper synchronization, scoped local sessions,
and email account/profile forms. These integration files are separate from
the vendored client assets and retain their applicable project licenses.
Comment storage and authentication are provided by the configured Waline
service; this client notice does not license that service or change any
upstream server license. Waline does not endorse or officially maintain this
SterSU integration.

Pinned client resource hashes, npm package integrity, and deployment scope
are documented in `docs/WALINE_THEME_COMMENTS.md`. Preserve the client license
and attribution when redistributing its assets.
