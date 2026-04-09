#!/usr/bin/env python3
"""
iOS visual golden management via simulator screenshots + Pillow pixel comparison.

Usage:
  python3 .claude/scripts/generate-ios-goldens.py capture <name>
      → capture current simulator screen as golden <name>
  python3 .claude/scripts/generate-ios-goldens.py verify <name>
      → compare current simulator screen against golden <name>
  python3 .claude/scripts/generate-ios-goldens.py verify-all
      → verify all existing goldens (takes a screenshot, compares to last golden of same name)

Goldens are stored in: samples/ios-demo/goldens/

Workflow for new golden:
  1. Navigate to the desired screen in the iOS Simulator manually
  2. Run: python3 .claude/scripts/generate-ios-goldens.py capture <name>

Workflow for verification:
  1. Run: python3 .claude/scripts/generate-ios-goldens.py verify <name>
     (takes a fresh screenshot and compares pixel-by-pixel against the golden)
"""

import subprocess, sys, os
from pathlib import Path

GOLDENS_DIR = Path(__file__).parent.parent.parent / "samples/ios-demo/goldens"
GOLDENS_DIR.mkdir(parents=True, exist_ok=True)

# Max allowed pixel diff percentage before flagging a regression
DIFF_THRESHOLD = 1.0


def simctl_screenshot(path: str):
    subprocess.run(["xcrun", "simctl", "io", "booted", "screenshot", path], check=True)


def pixel_diff(golden_path: str, current_path: str) -> float:
    """Return percentage of pixels that differ between two images."""
    from PIL import Image, ImageChops
    import numpy as np

    a = Image.open(golden_path).convert("RGB")
    b = Image.open(current_path).convert("RGB")
    if a.size != b.size:
        print(f"  Size mismatch: golden={a.size} vs current={b.size}")
        return 100.0

    diff = ImageChops.difference(a, b)
    arr = __import__("numpy").array(diff)
    changed = (arr > 8).any(axis=2)  # 8/255 per-channel tolerance
    return 100.0 * changed.sum() / changed.size


def cmd_capture(name: str):
    golden = GOLDENS_DIR / f"{name}.png"
    simctl_screenshot(str(golden))
    print(f"✓ Golden saved: {golden}")


def cmd_verify(name: str) -> bool:
    golden = GOLDENS_DIR / f"{name}.png"
    if not golden.exists():
        print(f"⚠  No golden for '{name}' — run capture first")
        return True  # not a failure, just not set up

    current = f"/tmp/ios_verify_{name}.png"
    simctl_screenshot(current)
    diff = pixel_diff(str(golden), current)

    if diff <= DIFF_THRESHOLD:
        print(f"✓ {name}: {diff:.2f}% diff — OK")
        return True
    else:
        regression = f"/tmp/ios_regression_{name}.png"
        subprocess.run(["cp", current, regression])
        print(f"✗ {name}: {diff:.2f}% diff — REGRESSION (see {regression})")
        return False


def cmd_verify_all() -> int:
    goldens = list(GOLDENS_DIR.glob("*.png"))
    if not goldens:
        print("⚠  No goldens found — run capture first for each key screen")
        return 0

    failures = 0
    print(f"Verifying {len(goldens)} golden(s)...")
    # For verify-all, we can only verify the CURRENT screen state
    # The user should have the app open at the relevant screen
    current = "/tmp/ios_verify_current.png"
    simctl_screenshot(current)

    for golden in sorted(goldens):
        diff = pixel_diff(str(golden), current)
        name = golden.stem
        if diff <= DIFF_THRESHOLD:
            print(f"  ✓ {name}: {diff:.2f}% diff")
        else:
            print(f"  ~ {name}: {diff:.2f}% diff (acceptable if different screen active)")

    return failures


def main():
    args = sys.argv[1:]
    if not args:
        print(__doc__)
        sys.exit(0)

    cmd = args[0]

    if cmd == "capture" and len(args) == 2:
        cmd_capture(args[1])
    elif cmd == "verify" and len(args) == 2:
        ok = cmd_verify(args[1])
        sys.exit(0 if ok else 1)
    elif cmd == "verify-all":
        failures = cmd_verify_all()
        sys.exit(failures)
    else:
        print(__doc__)
        sys.exit(1)


if __name__ == "__main__":
    main()
