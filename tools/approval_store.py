"""Register a local Java JAR in ZombieBuddy's approval store after a build."""
import hashlib
import json
import os
import tempfile
from pathlib import Path


def approve(mod_id, jar, backup_dir=None):
    jar = Path(jar).resolve()
    if not jar.is_file():
        raise FileNotFoundError(jar)
    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    store = Path.home() / '.zombie_buddy' / 'mod_approvals.json'
    data = json.loads(store.read_text()) if store.exists() else {'formatVersion': 2, 'mods': []}
    if data.get('formatVersion') not in (1, 2) or not isinstance(data.get('mods'), list):
        raise ValueError('Unknown ZombieBuddy approval store')
    if any(m.get('id') == mod_id and m.get('jar_hash') == digest and m.get('decision') is True
           for m in data['mods']):
        return False
    original = store.read_bytes() if store.exists() else None
    data['mods'].append({'id': mod_id, 'jar_hash': digest, 'decision': True})
    store.parent.mkdir(parents=True, exist_ok=True)
    if backup_dir is not None and original is not None:
        backup_dir = Path(backup_dir)
        backup_dir.mkdir(parents=True, exist_ok=True)
        (backup_dir / ('mod_approvals-' + digest[:12] + '.json')).write_bytes(original)
    with tempfile.NamedTemporaryFile(dir=store.parent, prefix='.approval-', delete=False) as tmp:
        tmp.write((json.dumps(data, ensure_ascii=False, indent=2) + '\n').encode())
        temporary = Path(tmp.name)
    os.replace(temporary, store)
    return True
