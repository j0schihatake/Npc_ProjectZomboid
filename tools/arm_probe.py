"""Arm a one-shot read-only NPC probe in a new verified save copy; never launch a game."""
import argparse
import base64
from datetime import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import secrets

from prepare_probe_world import prepare, require_closed_game, fingerprint

ROOT = Path(__file__).resolve().parents[1]
MAGIC = 'PersonalKnoxProbe1'


def atomic_write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + '.personalknox-tmp')
    temporary.write_bytes(data)
    os.replace(temporary, path)


def arm(userdata, source_name, seconds=180, quit_when_done=False):
    require_closed_game()
    if not 10 <= seconds <= 300:
        raise ValueError('Probe duration must be 10..300 seconds')
    lua = userdata / 'Lua'
    control = lua / 'PersonalKnox-probe.txt'
    if control.exists():
        state = control.read_text().splitlines()
        if len(state) >= 4 and state[0] == MAGIC and state[3] in ('armed', 'running'):
            raise ValueError('An existing probe is armed/running; inspect its process/record and clean up first')
    installed = userdata / 'mods/PersonalKnox/common/media/lua/client/PersonalKnox'
    expected = ROOT / 'common/media/lua/client/PersonalKnox/Probe.lua'
    if not (installed/'Probe.lua').is_file() or (installed/'Probe.lua').read_bytes() != expected.read_bytes():
        raise ValueError('Install the matching Probe adapter before arming a run')
    if 'Probe.install(' not in (installed/'Client.lua').read_text():
        raise ValueError('The installed Client does not install the Probe adapter')
    profile = userdata / 'mods/default.txt'
    profile_bytes = profile.read_bytes()
    enabled = set(re.findall(r'^\s*mod\s*=\s*(.+?)\s*,?\s*$', profile_bytes.decode(), re.M))
    required = {'PersonalKnox', 'ZombieBuddy', 'KnoxEventExpandedNpc', 'Viewpoint'}
    if not required <= enabled:
        raise ValueError('The current test profile lacks: ' + ', '.join(sorted(required-enabled)))
    latest = userdata/'latestSave.ini'
    latest_bytes = latest.read_bytes() if latest.exists() else None
    copy = prepare(userdata, source_name)
    baseline = json.loads((copy/'PersonalKnox-test.json').read_text())
    # Apply the actual current stack to the COPY, never to the original save.
    atomic_write(copy/'mods.txt', profile_bytes)
    nonce = secrets.token_hex(16)
    (copy/'PersonalKnox-probe-id.txt').write_text(nonce+'\n')
    record = {'nonce': nonce, 'copy': str(copy), 'source': baseline['source'],
              'source_sha256': baseline['sha256'], 'created': datetime.now().isoformat(),
              'profile_sha256': hashlib.sha256(profile_bytes).hexdigest(),
              'latest_before': base64.b64encode(latest_bytes).decode() if latest_bytes is not None else None,
              'seconds': seconds, 'quit_when_done': quit_when_done, 'scope': 'readonly_api'}
    require_closed_game()
    if profile.read_bytes() != profile_bytes:
        raise RuntimeError('The current mod profile changed; copy left unarmed')
    if fingerprint(Path(baseline['source'])) != baseline['sha256']:
        raise RuntimeError('The original save changed; copy left unarmed')
    if (latest.read_bytes() if latest.exists() else None) != latest_bytes:
        raise RuntimeError('The latest-save pointer changed; copy left unarmed')
    record_path = userdata/'PersonalKnox-probes'/f'{nonce}.json'
    atomic_write(record_path,(json.dumps(record,ensure_ascii=False,indent=2)+'\n').encode())
    atomic_write(lua/'PersonalKnox-probes'/f'{copy.name}.txt',(nonce+'\n').encode())
    require_closed_game()
    atomic_write(control,('\n'.join([MAGIC,copy.name,nonce,'armed',str(seconds),'1' if quit_when_done else '0'])+'\n').encode())
    return record_path


def cleanup(userdata, record_path):
    require_closed_game()
    record = json.loads(record_path.read_text())
    copy, source = Path(record['copy']), Path(record['source'])
    saves = userdata/'Saves/Sandbox'
    if copy.parent != saves or source.parent != saves or not copy.name.startswith('PersonalKnox-test-'):
        raise ValueError('Record is not bound to a local Sandbox test copy')
    if (copy/'PersonalKnox-probe-id.txt').read_text().strip() != record['nonce']:
        raise ValueError('Test copy identity no longer matches the record')
    unchanged = fingerprint(source) == record['source_sha256']
    latest = userdata/'latestSave.ini'
    current = latest.read_bytes() if latest.exists() else None
    rows = current.decode().splitlines() if current is not None else []
    require_closed_game()
    restored = False
    if rows[:2] == [copy.name,'Sandbox']:
        if latest.read_bytes() != current:
            raise RuntimeError('latestSave.ini changed during cleanup')
        if record['latest_before'] is None:
            latest.unlink()
        else:
            atomic_write(latest,base64.b64decode(record['latest_before']))
        restored = True
    control = userdata/'Lua/PersonalKnox-probe.txt'
    if control.exists():
        rows = control.read_text().splitlines()
        if len(rows)>=3 and rows[:3]==[MAGIC,copy.name,record['nonce']]:
            control.unlink()
    record.update(cleaned=datetime.now().isoformat(),source_unchanged=unchanged,latest_restored=restored)
    atomic_write(record_path,(json.dumps(record,ensure_ascii=False,indent=2)+'\n').encode())
    return unchanged, restored


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--userdata',type=Path,default=Path('/home/j0schi/Zomboid'))
    sub = parser.add_subparsers(dest='command',required=True)
    start = sub.add_parser('arm')
    start.add_argument('source',help='Source directory name under Saves/Sandbox; only a NEW copy is loaded')
    start.add_argument('--seconds',type=int,default=180)
    start.add_argument('--quit-when-done',action='store_true',help='Use the normal exit API after the token-bound test copy finishes')
    clean = sub.add_parser('cleanup')
    clean.add_argument('record',type=Path)
    args = parser.parse_args()
    userdata = args.userdata.resolve()
    if args.command=='arm':
        record = arm(userdata,args.source,args.seconds,args.quit_when_done)
        print('Armed a new test COPY for the next menu entry. Record:',record)
        print('default.txt and the original world were not changed. This command does not launch the game.')
    else:
        unchanged, restored = cleanup(userdata,args.record.resolve())
        print(f'Cleaned. Original world unchanged={unchanged}; latest-save pointer restored={restored}. Test copy retained.')
        if not unchanged:
            raise SystemExit(1)


if __name__=='__main__':
    main()
