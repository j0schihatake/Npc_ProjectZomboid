#!/usr/bin/env bash
# Existing test entry point, using the same isolated launcher as launch_dev.sh.
set -euo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec "$SCRIPT_DIR/launch_dev.sh" --restart --build --detach "$@"
