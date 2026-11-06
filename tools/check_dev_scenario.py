"""Scenario regression tests: map admission, untouched source, isolated copy and token-bound results."""
import json
from pathlib import Path
import sqlite3
import tempfile
from unittest.mock import patch
from dev_scenario import choose_save,prepare_scenario,wait_scenario
from dev_launch import atomic
from prepare_probe_world import fingerprint

with tempfile.TemporaryDirectory() as tmp:
    root=Path(tmp);userdata=root/'normal';saves=userdata/'Saves/Sandbox';saves.mkdir(parents=True)
    def save(name,maps):
        p=saves/name;p.mkdir()
        connection=sqlite3.connect(p/'players.db');connection.execute('CREATE TABLE localPlayers(isDead INTEGER)')
        connection.execute('INSERT INTO localPlayers VALUES(0)');connection.commit();connection.close()
        (p/'mods.txt').write_text('original mods')
        text=';'.join(maps)
        (p/'map_ver.bin').write_bytes((249).to_bytes(4,'big')+len(text).to_bytes(4,'big')+text.encode('utf-16-be'))
        return p
    vanilla=save('old',['Muldraugh, KY']);expanded=save('latest',['Missing City','Muldraugh, KY'])
    (userdata/'latestSave.ini').write_text('latest\nSandbox\n')
    assert choose_save(userdata,None,{'Muldraugh, KY'})==vanilla
    (userdata/'latestSave.ini').unlink()
    assert choose_save(userdata,None,{'Muldraugh, KY'})==vanilla,'Missing latest-save pointer must not block available templates'
    dead=save('zz-dead',['Muldraugh, KY'])
    connection=sqlite3.connect(dead/'players.db');connection.execute('UPDATE localPlayers SET isDead=1');connection.commit();connection.close()
    (userdata/'latestSave.ini').write_text('zz-dead\nSandbox\n')
    assert choose_save(userdata,None,{'Muldraugh, KY'})==vanilla,'A dead latest player must fall back to a living template'

    try:choose_save(userdata,expanded,{'Muldraugh, KY'});raise AssertionError('Missing map accepted')
    except ValueError:pass
    before=fingerprint(vanilla)
    cache=root/'dev';(cache/'mods').mkdir(parents=True);(cache/'Lua').mkdir()
    (cache/'mods/default.txt').write_text('dev profile')
    record=prepare_scenario(cache,vanilla,10,atomic)
    assert fingerprint(vanilla)==before
    assert (cache/'Saves/Sandbox'/record['world']/'mods.txt').read_text()=='dev profile'
    assert record['world']=='PersonalKnox-scenario-'+record['nonce']
    class Process:
        def poll(self):return 1
    path=cache/'Lua/PersonalKnox-dev-scenario-result.json'
    path.write_text(json.dumps(dict(record,status='OK',nonce='old-result')))
    assert wait_scenario(cache,record,Process(),10)==1,'Stale result must not pass'
    path.write_text(json.dumps(dict(record,status='FAIL',reason='save_validation_blocked')))
    assert wait_scenario(cache,record,Process(),10)==1
    path.write_text(json.dumps(dict(record,status='OK',reason='world_player_api_ticks')))
    assert wait_scenario(cache,record,Process(),10)==0
    jar=cache/'PersonalKnox.jar';jar.write_bytes(b'current build')
    import hashlib
    record['java_jars']={str(jar):hashlib.sha256(jar.read_bytes()).hexdigest()}
    assert wait_scenario(cache,record,Process(),10)==0
    jar.write_bytes(b'concurrent rebuild')
    assert wait_scenario(cache,record,Process(),10)==1,'A changed JAR must invalidate the result'
    record.pop('java_jars')
    (vanilla/'mods.txt').write_text('external mutation')
    assert wait_scenario(cache,record,Process(),10)==1
print('Scenario checks passed: map guards/fallback, independent copy, original hashes, nonce/world binding and error exit codes.')
