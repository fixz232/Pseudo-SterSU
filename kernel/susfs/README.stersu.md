# Optional SUSFS guard LKM

The files in this directory are vendored from
[KernelSU-Next/KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next)
commit `30932946f0ec6a69c2ce1e41109fa9bbf18aa384` (`kernel/susfs`),
verified on 2026-11-09. The previous snapshot was
`4c706b5938665866ebe16944a481a7b91e3396af`.
KernelSU-Next imported this LKM from
[inforcqb/susfs4ksu-lkm](https://github.com/inforcqb/susfs4ksu-lkm) at
`faf696a050b1c64009a4898cae7bcc0c9079f1d3`. That port credits
[simonpunk/susfs4ksu](https://gitlab.com/simonpunk/susfs4ksu) for SUSFS logic
and hook points and [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra)
for supporting hook/symbol code. Thanks to all upstream authors and contributors.

The upstream LKM project and these kernel files are GPL-2.0; the full license
text is at [`kernel/LICENSE`](../LICENSE). KernelSU-Next's repository-level
GPL-3.0 license does not replace these kernel-file notices. The source files
retain their upstream SPDX headers. This update includes the complete pinned
LKM source tree, including the upstream open-redirect fixes that supersede
the previously backported `b7c197621` and `bbe5db8f4` fixes.
The vendored `kallrecon/` symbol-resolution library retains its own GPL-2.0
license in `kallrecon/LICENSE` and original notices. Repository attributes
normalize `sus_path.c` line endings to LF. SterSU adds this attribution and
separate Manager/ksud build and policy wiring.
Preserve upstream notices
and review source and license changes before updating this copy. Neither
upstream project endorses SterSU.

This is a **separate** module named `susfs_guard_lkm.ko`. It does not replace
`kernelsu.ko` and is not the built-in GKI `CONFIG_KSU_SUSFS` implementation.
Only normal arm64 LKM mode can enable its autoload. Safe mode skips it, and
autoload is off by default. The Manager uses a separate LKM status page; the
existing GKI SUSFS configuration remains unchanged.

Build against the matching prepared Android DDK kernel tree:

```sh
make -C "$KDIR" M="$PWD" ARCH=arm64 CC=clang LLVM=1 LLVM_IAS=1 modules
llvm-strip -d susfs_guard_lkm.ko
```

The CI LKM job builds one guard KO per supported KMI and places it in the
arm64 ksud assets as `<kmi>_susfs_guard_lkm.ko`. The runtime selects the
detected KMI exactly; it never falls back to a different KMI after an OTA.
`ksud susfs lkm status` reports availability, load state, autoload state,
feature availability, LKM-only policy and the last boot-load error. `ksud susfs lkm enable` loads and verifies the
module before enabling boot autoload; `disable` turns off boot autoload and
`unload` also attempts to remove the running module.

The Manager's LKM page also exposes guard-specific logging, hiding of marked
mounts from non-root processes, uname release/build-version override and SUSFS
hidden paths. These use `ksud susfs lkm set-log`, `set-hide-mounts`, `set-uname`,
`clear-uname`, `add-path` and `remove-path`. Commands require the live LKM
feature marker and never fall back to the GKI interface. Settings are saved in
the separate `internal.ksud.susfsd` persistent module-config namespace and
replayed after guard autoload. GKI `/data/adb/ksu/susfs` settings, service
scripts and recovery generations are not read or modified. The kernel does not
expose live readback for every toggle, so the page reports saved policy and
surfaces command failures rather than claiming independent live verification.
The guard starts with logging enabled unless the LKM policy explicitly turns
it off; mount hiding starts disabled.

KMI compatibility is necessary but not sufficient for a modified vendor
kernel. Preserve a working boot image and a recovery path. A successful
compile is not a device boot or runtime validation.
