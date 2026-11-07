#!/usr/bin/env python3
"""Install one of the tracked profiles into the game's mods/default.txt.

The launch scripts are not touched. Pass --profile with-knox|no-knox.
"""
from __future__ import annotations
import argparse
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GAME = Path.home() / "Zomboid" / "mods" / "default.txt"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("profile", choices=["with-knox", "no-knox"])
    parser.add_argument("--game-profile", type=Path, default=GAME)
    args = parser.parse_args()
    source = ROOT / "profiles" / args.profile / "default.txt"
    if not source.is_file():
        raise SystemExit(f"missing profile: {source}")
    target = args.game_profile.expanduser()
    if target.exists():
        shutil.copy2(target, target.with_name(target.name + ".bak-profile"))
    shutil.copy2(source, target)
    print(f"installed {args.profile} -> {target}")
    if args.profile == "no-knox":
        print("launch with: ./tools/launch_dev.sh --profile full --restart --build --detach --no-debug --nosteam")


if __name__ == "__main__":
    main()
