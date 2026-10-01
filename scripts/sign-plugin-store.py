#!/usr/bin/env python3
"""Sign the plugin catalog with the private key kept outside the repository."""

from pathlib import Path
import base64
import os
import sys

from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization


def main() -> int:
    repo = Path(__file__).resolve().parents[1]
    catalog = repo / "plugin-store" / "catalog-v1.json"
    key_path = Path(os.environ.get("APKESU_PLUGIN_STORE_KEY", "")).expanduser()
    if not key_path.is_file():
        raise SystemExit(
            "Set APKESU_PLUGIN_STORE_KEY to the external Ed25519 PKCS#8 PEM key before signing."
        )
    key = serialization.load_pem_private_key(
        key_path.read_bytes(), password=None, backend=default_backend()
    )
    signature = base64.b64encode(key.sign(catalog.read_bytes())) + b"\n"
    for output in (
        repo / "plugin-store" / "catalog-v1.sig",
        repo / "manager" / "app" / "src" / "main" / "assets" / "plugin-store" / "catalog-v1.sig",
    ):
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_bytes(signature)
    print(f"Signed {catalog} -> {len(signature) - 1} base64 bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main())
