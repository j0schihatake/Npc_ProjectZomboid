"""Audit the current mod list with the selected maps; apply only while the game is closed."""
import argparse
from datetime import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

from check_map_cells import inspect

ROOT = Path(__file__).resolve().parents[1]
SELECTED = {
    'UnofficialMappersCommunityTilePack': 3628736763,
    'tikitown_tiles': 3046728955,
    'CamdenCountyB42': 3504080284,
    'tikitown': 3037854728,
    'Constown42': 3480990544,
    'RabbitHashKY': 2815966702,
    'RaccoonCityB42': 3388468313,
    'Frogtown': 3449473111,
    'FortBenningB42': 3490580478,
    'muldraughmilitarybaseas24': 3496507146,
    'Cathaya Valley 2.0 B42 version': 3576150391,
}


def expanded_text(base):
    enabled = set(re.findall(r'^\s*mod\s*=\s*(.+?)\s*,?\s*$', base, re.M))
    anchor = '    mod = ZombieBuddy,\n'
    if base.count(anchor) != 1:
        raise ValueError('Expected one ZombieBuddy entry; refusing to rewrite an unknown format')
    additions = ''.join('    mod = ' + name + ',\n' for name in SELECTED if name not in enabled)
    return base.replace(anchor, anchor + additions, 1)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true', help='Update default.txt after audit and backup')
    args = parser.parse_args()
    userdata = Path('/home/j0schi/Zomboid')
    current = userdata / 'mods/default.txt'
    original = current.read_bytes()
    candidate = expanded_text(original.decode('utf-8'))
    profiles = ROOT / 'profiles'
    profiles.mkdir(exist_ok=True)
    profile = profiles / 'ExpandedWorld-mods.txt'
    profile.write_text(candidate, encoding='utf-8')
    report = inspect(Path('/home/j0schi/SteamLibrary/steamapps/workshop/content/108600'), profile)
    (profiles / 'ExpandedWorld-audit.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    if report['overlaps'] or report['luaCollisions'] or report['tileCollisions']:
        raise SystemExit('Conflicts found; default.txt unchanged. See profiles/ExpandedWorld-audit.json')
    if any(m['missingEnabledRequirements'] for m in report['maps']):
        raise SystemExit('Missing map dependencies; default.txt unchanged')
    found = {name for m in report['maps'] for name in m['mods']}
    expected_maps = set(SELECTED) - {'tikitown_tiles', 'UnofficialMappersCommunityTilePack'}
    if expected_maps - found:
        raise SystemExit('Missing selected map files: ' + ', '.join(sorted(expected_maps - found)))
    cells = {tuple(cell) for m in report['maps'] for cell in m['cells']}
    print(f'Audited {len(report["maps"])} map folders, {len(cells)} distinct cells. Prepared {profile}')
    if not args.apply:
        print('Preview only; default.txt unchanged.')
        return
    if subprocess.run(['pgrep', '-f', '[P]rojectZomboid64'], capture_output=True).returncode != 1:
        raise SystemExit('Game is running; profile prepared, default.txt unchanged.')
    status = subprocess.run([sys.executable, str(ROOT / 'tools/workshop_item.py'), 'status',
                             *map(str, SELECTED.values())], check=True, capture_output=True, text=True)
    states = json.loads(status.stdout)['items']
    if not all(item['subscribed'] and item['installed'] and not item['needsUpdate']
               and not item['downloading'] and not item['pending'] for item in states.values()):
        raise SystemExit('Workshop subscriptions/downloads are not ready; default.txt unchanged')
    if current.read_bytes() != original:
        raise SystemExit('The mod list changed during audit; rerun before applying')
    if subprocess.run(['pgrep', '-f', '[P]rojectZomboid64'], capture_output=True).returncode != 1:
        raise SystemExit('Game started during audit; default.txt unchanged')
    backup = userdata / 'mod-setup-backup-20261105' / ('before-expanded-maps-' + datetime.now().strftime('%H%M%S-%f'))
    backup.mkdir(parents=True)
    shutil.copy2(current, backup / 'default.txt')
    (backup / 'original.sha256').write_text(hashlib.sha256(original).hexdigest() + '\n')
    temporary = current.with_name('default.txt.personalknox-tmp')
    temporary.write_text(candidate, encoding='utf-8')
    os.replace(temporary, current)
    print(f'Applied selected maps for new worlds. Backup: {backup}')


if __name__ == '__main__':
    main()
