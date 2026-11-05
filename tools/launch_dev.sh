#!/usr/bin/env bash
# Isolated PZ development launcher. Does not replace the normal mods/default.txt.
set -euo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec python3 "$SCRIPT_DIR/dev_launch.py" "$@"
