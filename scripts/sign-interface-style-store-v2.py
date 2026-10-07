#!/usr/bin/env python3
"""Sign the v2 style catalog without changing the legacy v1 trust root."""

import base64
import os
from pathlib import Path

from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization


def main() -> None:
    repo = Path(__file__).resolve().parents[1]
    catalog = repo / "interface-styles" / "catalog-v2.json"
    asset = repo / "manager/app/src/main/assets/interface-style/catalog-v2.json"
    public_key_file = repo / "interface-styles" / "catalog-v2.pub"
    key_name = os.environ.get("STERSU_INTERFACE_STYLE_V2_KEY")
    if not key_name or not Path(key_name).is_file():
        raise SystemExit("Set STERSU_INTERFACE_STYLE_V2_KEY to the external v2 Ed25519 PKCS#8 PEM key.")
    key = serialization.load_pem_private_key(
        Path(key_name).read_bytes(), password=None, backend=default_backend()
    )
    actual_public_key = key.public_key().public_bytes(
        serialization.Encoding.DER,
        serialization.PublicFormat.SubjectPublicKeyInfo,
    )
    expected_public_key = base64.b64decode(public_key_file.read_text(encoding="ascii").strip())
    if actual_public_key != expected_public_key:
        raise SystemExit("The v2 signing key does not match catalog-v2.pub.")

    catalog_bytes = catalog.read_bytes()
    if catalog_bytes != asset.read_bytes():
        raise SystemExit("The source and bundled v2 catalogs differ; regenerate them before signing.")
    signature = key.sign(catalog_bytes)
    key.public_key().verify(signature, catalog_bytes)
    encoded = base64.b64encode(signature) + b"\n"
    for output in (
        repo / "interface-styles" / "catalog-v2.sig",
        repo / "manager/app/src/main/assets/interface-style/catalog-v2.sig",
    ):
        output.write_bytes(encoded)
    print("Signed the matching source and bundled v2 style catalogs.")


if __name__ == "__main__":
    main()
