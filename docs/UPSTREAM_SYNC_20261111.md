# KernelSU upstream synchronization, 2026-11-11

## Plan and scope

- [x] Back up existing source, 16 shipping KOs, both Android userspace binaries and the previous APK.
- [x] Integrate seven upstream commits from `9ae6e65aa88a13a47f8e895550dd5b369c6d8820` to `dbb73665d103da40c7ecbd21ba3b5ac76eb95784`.
- [x] Preserve local profile atomic writes, root-command safety, custom themes, and independent GKI/LKM SUSFS.
- [x] Rebuild affected drivers/userspace and run component checks.
- [x] Build and verify the release APK, tests, signature, packaged assets, and hashes.

Keep Manager 3.3.3 / 33300, LKM 33300 and UAPI 6. Next LKM SUSFS remains at `30932946f0ec6a69c2ce1e41109fa9bbf18aa384`. The request includes no GitHub publishing or device installation/flashing. Audit and backups are under `out/upstream-sync-20261111/`; build outputs stay on E:/F:.

The new common preinit-runner linker flags also affect arm64; rebuild the eight arm64 KernelSU drivers. The eight separate SUSFS modules have unchanged sources and will be retained and hash-verified.

## Integrated changes

- Guard Manager URI launches with the upstream failure toast while preserving every custom theme, navigation host, and window setting.
- Existing Manager commands already quote the daemon path and all upstream-touched arguments. Retain those protections and add early module-action ID rejection, adapted to the existing Shell.Result API without starting a root shell.
- Profile names now reject CR/LF as well as existing traversal/NUL/backslash checks. Validate before filesystem side effects; retain strict policy application and atomic template/policy writes.
- Integrate x86_64 syscall fallback and seccomp-skip fixes and common preinit-runner ELF linker alignment flags.
- Integrate upstream installation documentation changes without removing SterSU's GKI integration.

## Component verification

- Android cargo check, clippy and cargo fmt passed with JDK 21 / NDK 29 tooling on Windows PowerShell.
- Production profile validation and atomic persistence regression: 3 passed in a host harness. Android-only policy/directory operations are unreachable in this harness; it does not claim device SELinux validation.
- GKI boot/safe-mode regressions: 9 passed under WSL Ubuntu with cc/GNU make. The first Windows attempt lacked these Linux prerequisites and was rerun in the supported environment.
- All eight arm64 KernelSU drivers rebuilt with existing WSL Docker DDK images. Verified all 16 KO ELF identities, KMI vermagic, driver 33300, UAPI 6, package/certificate pins, and runner ELF load alignment for 4/16/64 KiB pages.
- The eight independent SUSFS modules remain byte-identical to the previous verified payloads; their sources and policy integration are unchanged.
- A separate x86_64 preinit runner compiled with the new linker flags and exposes 64 KiB-aligned load segments. This is not a full x86_64 kernel-module boot test.
- Website build encounters exactly the same 48 pre-existing dead links as the previous verified baseline; no new dead links. APK/native checks do not depend on website publication.

## Release verification and delivery

- Clean Manager release build and unit tests completed successfully in 13m 49s.
- Manager tests: 1,264 total in 159 suites; 1,263 passed, one optional test skipped, zero failures/errors. Userspace tests: 69 passed.
- Verified both rebuilt ksud libraries, all 16 decompressed KO payloads, waitsys resources, and retained KernelPatch runtime inside the actual APK.
- Verified original package/certificate identity, non-debuggable release packaging, Manager UAPI 6, and 16 KiB ZIP alignment.
- All eight freshly rebuilt arm64 driver KOs are byte-identical to the previous verified payloads: the new explicit linker alignment matches the previous arm64 output. The x86_64 source/runner fixes are integrated without manufacturing changes to unchanged arm64 binaries.

APK: `release-local/3.3.3-33300-20261111/SterSU_3.3.3_33300-upstream-dbb73665d-release.apk`, 19,252,077 bytes.
SHA-256: `832cd2c32afde1f5c90cb4fef132d68664d5c0ac5535ddd9c55f3958702442e8`.

The adjacent `verification.json`, `ko-manifest.json` and `packaged-assets-verification.json` record final checks. No device installation, kernel flashing, hardware boot validation or GitHub publishing was performed. The separately recorded website build remains blocked by the same 48 pre-existing dead links, with no new dead links.
