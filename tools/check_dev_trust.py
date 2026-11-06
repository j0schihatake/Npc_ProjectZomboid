"""Check two-project standing trust without touching the real approval store."""
import json
from pathlib import Path
import tempfile
from dev_trust import policy,approve

with tempfile.TemporaryDirectory(prefix='pk-dev-trust-') as tmp:
    root=Path(tmp);cache=root/'cache';cache.mkdir()
    mods={};roots={}
    for name in ['PersonalKnox','PersonalWorldGen','Foreign']:
        folder=root/name;folder.mkdir();jar=folder/'mod.jar';jar.write_bytes(name.encode())
        roots[name]=folder;mods[name]={'root':folder,'java_jar':jar}
    assert policy(cache,roots)=={}
    trusted=policy(cache,roots,True)
    assert set(trusted)=={'PersonalKnox','PersonalWorldGen'}
    store=root/'approvals.json'
    original={'formatVersion':2,'mods':[{'id':'Foreign','jar_hash':'preserved','decision':False,'extra':'keep'}]}
    store.write_text(json.dumps(original));before=store.read_bytes()
    assert set(approve(mods,trusted,store,cache/'backups'))=={'PersonalKnox','PersonalWorldGen'}
    after=store.read_bytes();assert approve(mods,trusted,store,cache/'backups')==[] and store.read_bytes()==after
    assert json.loads(after)['mods'][0]==original['mods'][0]
    assert any(p.read_bytes()==before for p in (cache/'backups').iterdir())
    mods['PersonalKnox']['java_jar'].write_bytes(b'rebuilt code')
    assert approve(mods,policy(cache,roots),store,cache/'backups')==['PersonalKnox']
    other=root/'same-id-elsewhere';other.mkdir();(other/'mod.jar').write_bytes(b'untrusted root')
    assert approve({'PersonalKnox':{'root':other,'java_jar':other/'mod.jar'}},trusted,store,cache/'backups')==[]
    escaped=roots['PersonalKnox']/'escape.jar';escaped.symlink_to(other/'mod.jar')
    before=store.read_bytes()
    try:
        approve({'PersonalKnox':{'root':roots['PersonalKnox'],'java_jar':escaped}},trusted,store,cache/'backups')
        raise AssertionError('JAR outside the trusted source root accepted')
    except ValueError:
        pass
    assert store.read_bytes()==before
print('Dev trust checks passed: two independent projects, changed hashes, persistent root policy, backups, no foreign changes, wrong-root rejection and no escaping JAR paths.')
