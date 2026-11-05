#!/usr/bin/env bash
# Launch PZ without a timeout: the B42 world load can exceed 5 minutes with many maps/NPCs.
set -euo pipefail
cd /home/j0schi/SteamLibrary/steamapps/common/ProjectZomboid
exec ./projectzomboid.sh "$@"
