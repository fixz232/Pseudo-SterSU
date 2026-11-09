# Upstream synchronization, 2026-11-09

## Scope and plan

- [x] Verify upstream revisions, inventory local work, and back up shipped
  modules, userspace binaries, APK, and uncommitted changes.
- [x] Integrate tiann/KernelSU main at
  `9ae6e65aa88a13a47f8e895550dd5b369c6d8820`, using the previously integrated
  `528bafc5696196f3a9500cee2e7e18497421cd32` as the three-way merge base.
- [x] Update the independent LKM SUSFS snapshot from KernelSU-Next dev at
  `30932946f0ec6a69c2ce1e41109fa9bbf18aa384`, retaining local integration and
  attribution. Keep built-in GKI SUSFS independent.
- [x] Rebuild and verify all eight KernelSU drivers and eight optional SUSFS
  modules in isolated build directories before replacing shipping payloads.
- [x] Run affected component checks, rebuild both bundled Android userspace
  binaries, build the release APK, run Manager tests, and verify the actual
  packaged payloads, version, signature, alignment, and hashes.

Retain Manager 3.3.3 / LKM 33300, UAPI 6, the current package and signing
identity, and all existing uncommitted UI changes. This request does not
include GitHub publishing, device installation, or kernel flashing.

## Evidence and backups

Audit directory: `out/upstream-sync-20261109/`. It contains the pre-change
source/artifact archive, per-file hashes, original worktree patch/status,
upstream CI responses, three-way merge inputs/results, build logs, and final
verification records. Build resources and outputs remain on E:/F:.

Compilation and static checks cannot establish device boot compatibility;
hardware boot/runtime validation must be reported separately.

## Integration and checks

- Both upstream heads were verified using direct Git refs and the GitHub API.
  tiann's latest Manager CI run `42573119414` successfully built all eight
  KernelSU KMI targets. Next's run `42579196274` successfully built all eight
  standalone SUSFS targets. The local binaries are rebuilt with SterSU's
  existing package/certificate identity, rather than using foreign prebuilt
  drivers. Next's separate formatting workflow fails upstream; no kernel
  build failure was reported by that run.
- Kernel changes include BTF-derived SELinux inode offsets (notably for
  6.6.144 ABI drift), SELinux policy generation/original-type tracking,
  wrapped-file reference balancing, and the arm64 7.3 syscall argument fix.
- The complete pinned LKM SUSFS update includes kallrecon, BTF/fsnotify
  lifetime handling, directory/create/mount layers, feature initialization
  gating, and open-redirect fixes. The existing GKI built-in integration,
  separate LKM runtime policy, safe-mode skip, and opt-in autoload remain.
- Manager/JS updates include ZIP MIME types, shared package scans and the
  versioned PM/File APIs, stream limits/JSON bridge results, WebUI download
  transport, cutout insets, and theme-specific WebUI dialogs. Keep local
  embedded KPM theme/wallpaper integration and generation-aware disposal.
- While integrating the new chunked-download bridge, fixed its URL/session
  key mismatch and explicitly publish completed files through MediaStore.
  Retain the legacy native download entry point and add regression tests
  for handle validation, completion, cleanup, and cancellation.
- The customized static website/deployment and removed upstream advertising
  stay local. Upstream website dependencies are updated. Its documentation
  build still reports exactly the same 48 pre-existing dead-link occurrences
  as the previous audit, with no new dead links; this does not affect APK/KO
  builds. See `website-baseline-comparison.json`.
- All 16 KOs compiled in fresh isolated WSL Docker builds on E:. Verified
  ELF64/AArch64 ET_REL, KMI vermagic, empty CRC tables, identity, driver 33300,
  UAPI 6, preinit runner, new BTF symbols, and new SUSFS/kallrecon symbols.
- GKI boot/safe-mode regression: 9 passed. JS syntax checks passed. Android
  cargo check, clippy, and fmt passed. Both Android ksud binaries were rebuilt
  with the new KO payloads; all 69 userspace unit tests passed.

## Final delivery

- The first clean Manager build exposed an import conflict and duplicated
  system-bar helpers during WebUI bridge extraction. Both were fixed; the
  successful release/test retry completed in 8m 48s. Logs from both attempts
  are preserved in the audit directory.
- Manager tests: 1,264 in 159 suites, 1,263 passed, one optional test skipped,
  zero failures/errors. The four new chunked-download regression tests pass.
- Verified the actual release APK's 16 decompressed KO payloads against the
  fresh build hashes, both rebuilt/stripped ksud libraries, both waitsys
  resources, and retained KernelPatch runtime resources.
- Confirmed package `io.github.fixz.stersu`, version 3.3.3 / 33300, UAPI 6,
  non-debuggable Release packaging, the original signing certificate/pin,
  ZIP MIME additions, retained weather queries, removed receiver permission,
  and 16 KiB ZIP alignment.
- All 56 archived pre-existing Manager source edits remain byte-identical.
  The 94 pinned SUSFS source files match upstream (line endings normalized),
  and the exact build source archive contains every one of them.

Artifacts in `release-local/3.3.3-33300-20261109/`:

- `SterSU_3.3.3_33300-upstream-9ae6e65aa-release.apk`, 19,251,973 bytes.
  SHA-256: `c2522485d067ac9d59a3d511ebb776d00c06fc36aa8338c698437ba239cba406`.
- `SterSU_LKM_33300_KO_20261109.zip`: 16 new raw KOs, exact corresponding
  kernel source, licenses/notices, build recipe, and verification manifests.
  SHA-256: `67d5e198694b88e138e59f0a6e83c0a614215421360c9cf9d6eee5225352c4bb`.
- `SterSU_LKM_33300_previous_KO_20261109.zip`: all 16 previously shipped
  KOs and their hashes, extracted from the pre-sync backup. Neither KO ZIP
  is a recovery-flashable package.
- `verification.json`, `ko-manifest.json`, and
  `packaged-assets-verification.json` record final checks and provenance.

No device installation/flashing or physical boot/runtime test was performed.
The separately documented 48 pre-existing website dead links remain; there
are no new documentation dead links from the dependency update.
