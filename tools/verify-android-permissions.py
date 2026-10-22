#!/usr/bin/env python3
"""Compare APK uses-permission entries with the audited docs/permisos.md list."""

from __future__ import annotations

import os
import re
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
BUILD_TOOLS = "37.0.0"


def android_sdk() -> Path | None:
    configured = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if configured:
        return Path(configured).expanduser()

    properties = ROOT / "local.properties"
    if properties.exists():
        for line in properties.read_text(encoding="utf-8").splitlines():
            if line.startswith("sdk.dir="):
                value = line.removeprefix("sdk.dir=").replace("\\:", ":").replace("\\\\", "\\")
                return Path(value).expanduser()
    return None


def documented_permissions(document: Path) -> list[str]:
    permissions: list[str] = []
    for line in document.read_text(encoding="utf-8").splitlines():
        cells = line.strip().split("|")
        if len(cells) < 3:
            continue
        match = re.fullmatch(r"\s*`([^`]+)`\s*", cells[1])
        if match:
            permissions.append(match.group(1))
    return permissions


def main() -> int:
    sdk = android_sdk()
    if sdk is None:
        print("FAIL No encuentro Android SDK: define ANDROID_HOME/ANDROID_SDK_ROOT o local.properties.")
        return 2

    aapt = sdk / "build-tools" / BUILD_TOOLS / "aapt"
    apk = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
    document = ROOT / "docs/permisos.md"
    for path in (aapt, apk, document):
        if not path.is_file():
            print(f"FAIL Falta {path.relative_to(ROOT) if path.is_relative_to(ROOT) else path}; compila el APK debug y reintenta.")
            return 2

    result = subprocess.run([str(aapt), "dump", "badging", str(apk)], capture_output=True, text=True, check=False)
    if result.returncode != 0:
        print(f"FAIL aapt no pudo leer el APK: {result.stderr.strip()}")
        return 2

    actual = re.findall(r"^uses-permission: name='([^']+)'", result.stdout, flags=re.MULTILINE)
    documented = documented_permissions(document)
    if len(documented) != len(set(documented)):
        print("FAIL docs/permisos.md repite un permiso en la tabla.")
        return 1

    actual_set = set(actual)
    documented_set = set(documented)
    missing = sorted(actual_set - documented_set)
    stale = sorted(documented_set - actual_set)
    if missing or stale:
        if missing:
            print("FAIL Permisos presentes en el APK y ausentes del inventario:")
            for permission in missing:
                print(f"  + {permission}")
        if stale:
            print("FAIL Permisos documentados que no están en el APK debug:")
            for permission in stale:
                print(f"  - {permission}")
        return 1

    print(f"PASS Los {len(actual_set)} permisos del APK debug coinciden con docs/permisos.md.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
