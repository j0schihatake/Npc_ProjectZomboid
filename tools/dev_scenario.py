"""Disposable Sandbox smoke scenario and token-bound result protocol."""
import json
import hashlib
import re
from pathlib import Path
import secrets
import shutil
import sqlite3
import time
import subprocess

from prepare_probe_world import fingerprint


def saved_maps(source):
    data=(source/'map_ver.bin').read_bytes()
    count=int.from_bytes(data[4:8],'big')
    if len(data)<8 or not 1<=count<=10000 or len(data)<8+count*2:
        raise ValueError('Unknown map_ver.bin header: '+str(source))
    return [name.strip() for name in data[8:8+count*2].decode('utf-16-be').split(';') if name.strip()]


def choose_save(userdata, explicit=None, available_maps=None):
    if not explicit:
        candidates=[]
        latest=userdata/'latestSave.ini'
        if latest.is_file():
            rows=latest.read_text().splitlines()
            if len(rows)>=2 and rows[1]=='Sandbox' and Path(rows[0]).name==rows[0]:
                candidates.append(userdata/'Saves/Sandbox'/rows[0])
        saves=userdata/'Saves/Sandbox'
        if saves.is_dir():
            candidates.extend(p for p in sorted(saves.iterdir(),reverse=True)
                              if not p.name.startswith('PersonalKnox-') and p not in candidates)
        for candidate in candidates:
            try:
                return choose_save(userdata,candidate,available_maps)
            except (ValueError,OSError,sqlite3.Error):
                continue
        raise ValueError('No living-player Sandbox save matches the selected maps; pass --profile full or --scenario-save')
    source=Path(explicit).expanduser().resolve()
    if source.is_symlink() or not source.is_dir() or not (source/'players.db').is_file() or not (source/'mods.txt').is_file():
        raise ValueError('Scenario needs a complete closed Sandbox save: '+str(source))
    if available_maps is not None:
        missing=set(saved_maps(source))-available_maps
        if missing:
            raise ValueError('Scenario source needs disabled/missing maps: '+', '.join(sorted(missing)))
    connection=sqlite3.connect((source/'players.db').as_uri()+'?mode=ro',uri=True)
    try:
        if connection.execute('PRAGMA quick_check').fetchall()!=[('ok',)]:
            raise ValueError('Scenario source player database is damaged')
        if not connection.execute('SELECT 1 FROM localPlayers WHERE isDead=0 LIMIT 1').fetchone():
            raise ValueError('Scenario source has no living player; pass another --scenario-save')
    finally:
        connection.close()
    return source


def prepare_scenario(cache, source, seconds, atomic):
    nonce=secrets.token_hex(16)
    name='PersonalKnox-scenario-'+nonce
    saves=cache/'Saves/Sandbox';saves.mkdir(parents=True,exist_ok=True)
    target=saves/name
    before=fingerprint(source)
    shutil.copytree(source,target)
    if fingerprint(source)!=before or fingerprint(target)!=before:
        raise ValueError('Save changed during copying; scenario left unarmed')
    shutil.copy2(cache/'mods/default.txt',target/'mods.txt')
    record={'nonce':nonce,'world':name,'source':str(source),'source_sha256':before,'seconds':seconds}
    atomic(cache/'dev-scenario.json',json.dumps(record,ensure_ascii=False,indent=2)+'\n')
    atomic(cache/'Lua/PersonalKnox-dev-scenario.txt',
           '\n'.join(['PersonalKnoxDevScenario1',name,nonce,str(seconds),'armed'])+'\n')
    return record


def runtime_errors(text, nonce):
    """Only inspect the current scenario; do not confuse vanilla warnings with addon exceptions."""
    if 'run='+nonce not in text:
        return []
    errors=[]
    own_frame=r'Lua\(\(MOD:Personal[ -]?Knox\b[^\r\n]*'
    for block in re.split(r'(?=^(?:ERROR|LOG|WARN|DEBUG)\s*:)',text,flags=re.M):
        if block.startswith('ERROR'):
            match=re.search(own_frame,block,re.I)
            if match and match.group() not in errors:
                errors.append(match.group())
    for match in re.finditer(r'\[PersonalKnox\][^\r\n]*\berror:[^\r\n]*',text,re.I):
        if match.group() not in errors: errors.append(match.group())
    return errors[:3]


def wait_scenario(cache, record, process, timeout):
    deadline=time.monotonic()+timeout
    result_path=cache/'Lua/PersonalKnox-dev-scenario-result.json'
    next_notice=time.monotonic()+30
    while True:
        try:
            result=json.loads(result_path.read_text())
        except (FileNotFoundError,json.JSONDecodeError):
            result={}
        if result.get('nonce')==record['nonce'] and result.get('world')==record['world']:
            if result.get('status')=='OK':
                console=cache/'console.txt'
                errors=runtime_errors(console.read_text(errors='replace') if console.exists() else '',record['nonce'])
                if errors:
                    print('FAIL: PersonalKnox runtime errors: '+'; '.join(errors),flush=True);return 1
                jars=dict(record.get('java_jars',{}))
                agent=record.get('scenario_agent')
                if agent: jars[agent['path']]=agent['sha256']
                for path,digest in jars.items():
                    if not Path(path).is_file() or hashlib.sha256(Path(path).read_bytes()).hexdigest()!=digest:
                        print('FAIL: a tested JAR changed during the scenario: '+path,flush=True);return 1
                if fingerprint(Path(record['source']))!=record['source_sha256']:
                    print('FAIL: scenario source changed during the run',flush=True);return 1
                print('OK: Sandbox loaded, living player, PersonalKnox API and game updates verified. World: '+record['world'],flush=True)
                return 0
            print('FAIL: '+result.get('reason','scenario_failed'),flush=True);return 1
        if process.poll() is not None:
            print('FAIL: game exited before scenario completion',flush=True);return 1
        if time.monotonic()>=deadline:
            print('FAIL: scenario timeout; inspect '+str(cache/'console.txt')+' (game left available for inspection)',flush=True)
            return 2
        if time.monotonic()>=next_notice:
            print('Waiting for scenario world/result...',flush=True);next_notice=time.monotonic()+30
        time.sleep(0.5)


def build_agent(cache, root):
    folder=cache/'scenario-agent';folder.mkdir(exist_ok=True)
    classes=folder/'classes';classes.mkdir(exist_ok=True)
    subprocess.run(['javac','--release','17','-d',str(classes),str(root/'tools/dev_scenario_agent/ScenarioAgent.java')],check=True)
    jar=folder/'ScenarioAgent.jar'
    manifest=folder/'MANIFEST.MF';manifest.write_text('Manifest-Version: 1.0\nPremain-Class: ScenarioAgent\nAgent-Class: ScenarioAgent\n\n')
    subprocess.run(['jar','cfm',str(jar),str(manifest),'-C',str(classes),'.'],check=True)
    return jar
