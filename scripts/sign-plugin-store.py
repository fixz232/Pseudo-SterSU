#!/usr/bin/env python3
"""Sign the plugin catalog with the private key kept outside the repository."""

from pathlib import Path
import base64
import os
import re
import sys

from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey


def main() -> int:
    repo = Path(__file__).resolve().parents[1]
    version = sys.argv[1] if len(sys.argv) == 2 else "1"
    if len(sys.argv) > 2 or version not in {"1", "2"}:
        raise SystemExit("Usage: sign-plugin-store.py [1|2]")
    filename = f"catalog-v{version}"
    catalog = repo / "plugin-store" / f"{filename}.json"
    key_path = Path(os.environ.get("APKESU_PLUGIN_STORE_KEY", "")).expanduser()
    if not key_path.is_file():
        raise SystemExit(
            "Set APKESU_PLUGIN_STORE_KEY to the external Ed25519 PKCS#8 PEM key before signing."
        )
    key = serialization.load_pem_private_key(
        key_path.read_bytes(), password=None, backend=default_backend()
    )
    if not isinstance(key, Ed25519PrivateKey):
        raise SystemExit("The plugin catalog requires an Ed25519 private key.")
    manager = repo / "manager/app/src/main/java/me/weishu/kernelsu/ui/util/PluginStore.kt"
    constant = "LEGACY_CATALOG_PUBLIC_KEY_B64" if version == "1" else "CATALOG_PUBLIC_KEY_B64"
    match = re.search(
        rf'^private const val {constant} = "([A-Za-z0-9+/=]+)"$',
        manager.read_text(encoding="utf-8"),
        re.MULTILINE,
    )
    public_key = key.public_key().public_bytes(
        serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo
    )
    if not match or base64.b64encode(public_key).decode("ascii") != match.group(1):
        raise SystemExit(f"Private key does not match the Manager's pinned v{version} catalog key.")
    bundled = repo / "manager/app/src/main/assets/plugin-store" / f"{filename}.json"
    payload = catalog.read_bytes()
    if bundled.read_bytes() != payload:
        raise SystemExit("Published and bundled catalogs differ; regenerate both before signing.")
    signature = base64.b64encode(key.sign(payload)) + b"\n"
    for output in (
        repo / "plugin-store" / f"{filename}.sig",
        repo / "manager" / "app" / "src" / "main" / "assets" / "plugin-store" / f"{filename}.sig",
    ):
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_bytes(signature)
    print(f"Signed {catalog} -> {len(signature) - 1} base64 bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main())
