#!/usr/bin/env python3
"""Enable in-game NPC tests, launch the standard dev script, collect PASS/FAIL."""
from __future__ import annotations
import os
import re
import signal
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CACHE = Path(os.environ.get("PZ_DEV_CACHE", "/home/j0schi/Zomboid-dev/PersonalKnox"))
OPTIONS = CACHE / "Lua" / "ModOptions.ini"
CONSOLE = CACHE / "console.txt"
OUT = ROOT / "build" / "ingame-tests.log"
KEY = "tickbox|PersonalKnox|inGameTests|"


def set_enabled(enabled: bool, original: str | None = None) -> str:
    if original is not None:
        OPTIONS.write_text(original, encoding="utf-8")
        return original
    text = OPTIONS.read_text(encoding="utf-8") if OPTIONS.exists() else ""
    lines = [line for line in text.splitlines() if not line.startswith(KEY)]
    lines.append(KEY + ("true" if enabled else "false"))
    updated = "\n".join(lines) + "\n"
    OPTIONS.write_text(updated, encoding="utf-8")
    return text


def main() -> int:
    if not OPTIONS.exists():
        print(f"missing ModOptions: {OPTIONS}", file=sys.stderr)
        return 2
    original = OPTIONS.read_text(encoding="utf-8")
    try:
        set_enabled(True)
        result = subprocess.run([str(ROOT / "tools" / "launch_dev.sh")], cwd=ROOT,
                                capture_output=True, text=True, timeout=1200)
        text = CONSOLE.read_text(encoding="utf-8", errors="replace") if CONSOLE.exists() else ""
        rows = [line for line in text.splitlines() if "[ingame-test]" in line]
        pid_match = re.search(r"^PID: (\d+)$", result.stdout, re.MULTILINE)
        if pid_match:
            pid = int(pid_match.group(1))
            try:
                cmdline = Path(f"/proc/{pid}/cmdline").read_bytes().replace(b"\0", b" ").decode("utf-8", "replace")
                if str(CACHE) in cmdline:
                    os.kill(pid, signal.SIGTERM)
                    for _ in range(30):
                        if not Path(f"/proc/{pid}").exists():
                            break
                        time.sleep(0.1)
            except (OSError, ValueError):
                pass
        summary = next((line for line in reversed(rows) if "summary passed=" in line), "")
        match = re.search(r"passed=(\d+) failed=(\d+)", summary)
        OUT.parent.mkdir(parents=True, exist_ok=True)
        report = "\n".join(rows) + ("\n" if rows else "")
        OUT.write_text(report, encoding="utf-8")
        print(report, end="")
        print(f"report: {OUT}")
        if not match:
            print("no in-game test summary in console.txt", file=sys.stderr)
            return 1
        passed, failed = int(match.group(1)), int(match.group(2))
        print(f"in-game tests: passed={passed} failed={failed}")
        return 0 if failed == 0 else 1
    finally:
        set_enabled(False, original)


if __name__ == "__main__":
    raise SystemExit(main())
