"""Standing development trust for exactly two local projects; bridge to ZB's hash store."""
from datetime import datetime
import hashlib
import json
import os
from pathlib import Path
import tempfile

PROJECTS={'PersonalKnox','PersonalWorldGen'}


def policy(cache, roots, authorize=False):
    path=cache/'.local-mod-trust.json'
    if authorize:
        data={'version':1,'projects':{key:str(Path(value).resolve()) for key,value in roots.items() if key in PROJECTS}}
        path.write_text(json.dumps(data,indent=2)+'\n')
    elif path.exists():
        data=json.loads(path.read_text())
    else:
        return {}
    if data.get('version')!=1 or not isinstance(data.get('projects'),dict) or set(data['projects'])-PROJECTS:
        raise ValueError('Unknown local project trust policy')
    return data['projects']


def approve(mods, trusted, store, backup_dir):
    candidates=[]
    for identifier,mod in mods.items():
        if identifier not in PROJECTS or identifier not in trusted:
            continue
        root=mod['root'].resolve()
        if root!=Path(trusted[identifier]).resolve():
            continue
        jar=mod.get('java_jar')
        if jar is None:
            continue
        jar=jar.resolve()
        if not jar.is_relative_to(root) or not jar.is_file():
            raise ValueError('Trusted project JAR must exist inside its source root: '+identifier)
        candidates.append((identifier,jar,hashlib.sha256(jar.read_bytes()).hexdigest()))
    if not candidates:
        return []
    original=store.read_bytes() if store.exists() else None
    data=json.loads(original) if original is not None else {'formatVersion':2,'mods':[]}
    if data.get('formatVersion') not in (1,2) or not isinstance(data.get('mods'),list):
        raise ValueError('Unknown ZombieBuddy approval store format')
    changed=[]
    for identifier,jar,digest in candidates:
        previous=[entry.get('decision') for entry in data['mods'] if entry.get('jar_hash')==digest]
        if previous and previous[-1] is True:
            continue
        data['mods'].append({'id':identifier,'jar_hash':digest,'decision':True})
        changed.append(identifier)
    if not changed:
        return []
    for _,jar,digest in candidates:
        if hashlib.sha256(jar.read_bytes()).hexdigest()!=digest:
            raise ValueError('A trusted JAR changed during approval; finish the build first')
    backup_dir.mkdir(parents=True,exist_ok=True)
    if original is not None:
        backup=backup_dir/('mod_approvals-'+datetime.now().strftime('%Y%m%d-%H%M%S-%f')+'.json')
        backup.write_bytes(original);backup.chmod(0o600)
    store.parent.mkdir(parents=True,exist_ok=True)
    with tempfile.NamedTemporaryFile(dir=store.parent,prefix='.local-approval-',delete=False) as tmp:
        staged=Path(tmp.name)
        tmp.write((json.dumps(data,indent=2)+'\n').encode())
    try:
        if (store.read_bytes() if store.exists() else None)!=original:
            raise ValueError('Approval store changed concurrently; retry after the other writer finishes')
        os.replace(staged,store)
    finally:
        staged.unlink(missing_ok=True)
    return changed
