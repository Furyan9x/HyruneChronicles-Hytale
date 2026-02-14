#!/usr/bin/env python3
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ITEMS_DIR = ROOT / "src" / "main" / "resources" / "Server" / "Item" / "Items"


def main() -> int:
    if not ITEMS_DIR.exists():
        print(f"Items directory not found: {ITEMS_DIR}")
        return 1

    changed = 0
    inspected = 0
    skipped = 0

    for file_path in ITEMS_DIR.rglob("*.json"):
        inspected += 1
        try:
            with file_path.open("r", encoding="utf-8") as f:
                data = json.load(f)
        except Exception:
            skipped += 1
            continue

        if not isinstance(data, dict):
            skipped += 1
            continue

        if "Quality" not in data:
            continue

        if data.get("Quality") == "Common":
            continue

        data["Quality"] = "Common"
        with file_path.open("w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=4)
            f.write("\n")
        changed += 1

    print(f"Inspected: {inspected}")
    print(f"Changed:   {changed}")
    print(f"Skipped:   {skipped}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

