#!/usr/bin/env bash
# Minimal test profile: only ZombieBuddy, Viewpoint, Knox and PersonalKnox.
# The real mods/default.txt is restored when the game exits.
set -euo pipefail
GAME=/home/j0schi/SteamLibrary/steamapps/common/ProjectZomboid
ZOMBIOD="$HOME/Zomboid"
DEFAULT="$ZOMBIOD/mods/default.txt"
BACKUP="$DEFAULT.bak-personaltest"
[[ -f "$DEFAULT" ]] || { echo "No $DEFAULT"; exit 1; }
[[ -f "$BACKUP" ]] || cp -p "$DEFAULT" "$BACKUP"
restore() { cp -p "$BACKUP" "$DEFAULT"; }
trap restore EXIT INT TERM
cat > "$DEFAULT" <<'MODS'
VERSION = 1,
mods
{
    mod = ZombieBuddy,
    mod = Viewpoint,
    mod = KnoxEventExpandedNpc,
    mod = KnoxEventExpandedNpcRU,
    mod = PersonalKnox,
}
maps
{
}
MODS
cd "$GAME"
./projectzomboid.sh -debug -nosteam
