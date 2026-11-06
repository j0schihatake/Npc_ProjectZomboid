#!/usr/bin/env bash
# Isolated PZ development launcher. Does not replace the normal mods/default.txt.
set -euo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
if ! protocol=$(python3 "$SCRIPT_DIR/dev_launch.py" --launcher-protocol 2>/dev/null) || [[ "$protocol" != "PersonalKnoxDevLauncher2" ]]; then
  echo "Dev launch: incompatible or incomplete launcher files. Restore the current tools from Git; an old dev_launch.py cannot run the automatic world scenario." >&2
  exit 1
fi
exec python3 "$SCRIPT_DIR/dev_launch.py" "$@"
