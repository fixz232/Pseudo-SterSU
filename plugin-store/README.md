# SterSU Manager plugins

The Manager reads this directory from `fixz232/Pseudo-SterSU/main`.
Plugin packages are data-only capability descriptors. The actual pages and
runtime implementation are supplied by a compatible Manager and ksud, not by
executable code downloaded from this directory.

## Catalog compatibility

- `catalog-v1.json` and `catalog-v1.sig` preserve the signed nine-plugin
  catalog for older Manager builds. Do not append entries to that catalog.
- `catalog-v2.json` prepares the ten-plugin catalog, including
  `packages/pathmask-lkm.ksplugin` (LKM hidden-path configuration).
- The new plugin requires Manager and ksud version code 33000 or newer,
  LKM mode, and the supported Pathmask runtime on the device.
- Remote activation requires `catalog-v2.sig`, signed by the existing plugin
  catalog Ed25519 private key. Until that signature is published, the prepared
  v2 catalog is not an installable store release. The Manager must continue
  rejecting unsigned or incorrectly signed catalogs.

## Publishing v2

1. Run `scripts/generate-plugin-store-v2.ps1` from the source repository to
   calculate the package hash and byte count and update both catalog copies.
2. Set `APKESU_PLUGIN_STORE_KEY` to the external PKCS#8 PEM file whose public
   key matches `CATALOG_PUBLIC_KEY_B64` in `PluginStore.kt`.
3. Run `python scripts/sign-plugin-store.py 2`.
4. Verify the signed Git blob and all package hashes before publishing the
   catalog, signature, and package together in one commit.
5. Re-download the public files and verify them again.

Never commit the private key, copy the v1 signature onto v2, disable signature
checks, or replace the trusted public key without a separate migration plan.
The web-manager and stealth capabilities remain paired in
`remote-management-suite`; they are not separate independent downloads.
