from check_run import summarize, startup_matches
from pathlib import Path
import subprocess
import sys
import tempfile
import re

log = '''
[PersonalKnox] Java core 0.4.6 loaded; directional world reactions ready.
[PersonalKnox] Lua addon loaded; Russian dialogues and elevation diagnostics ready.
[PersonalKnox][capabilities] luaNpcTransferAction=false luaBehaviorNode=true luaSubTreeNode=true
[PersonalKnox][care] action=bandage ok=true bleeding=1 pain=10.0 thirst=0.10 hunger=0.20
[PersonalKnox][upkeep] ammo=Base.Bullets9mm add=30
[PersonalKnox][probe] npc=3129 windows=12
'''
report = summarize(log)
assert report['core'] == '0.4.6'
assert report['lua'] and report['capabilities'] == 1
assert report['lua_version'] == ''
assert not startup_matches(report,'0.4.6','0.4.6'), 'Legacy Lua loading is not proof of its release'
assert report['care'] == 1 and report['upkeep'] == 1 and report['probe'] == 1
assert not report['errors'] and not report['exited']
bad = summarize('[PersonalKnox] Java core 0.4.5 loaded\n[PersonalKnox] Care error: boom')
assert bad['core'] == '0.4.5' and bad['errors']
versioned=log.replace('Lua addon loaded','Lua addon 0.4.6 loaded')
assert startup_matches(summarize(versioned),'0.4.6','0.4.6')
assert not startup_matches(summarize(versioned),'0.4.6','0.4.53'), 'Wrong Lua must fail even when Java matches'
mixed=versioned.replace('Lua addon 0.4.6 loaded','Lua addon 0.4.7 loaded')
assert not startup_matches(summarize(mixed),'0.4.6','0.4.6')
assert startup_matches(summarize(mixed),'0.4.6','0.4.7'), 'Explicit separate core/Lua versions are supported'
assert not startup_matches(summarize(versioned+'\n[PersonalKnox] Lua addon loaded'),'0.4.6','0.4.6'), 'Use the last Lua load'
assert not startup_matches(summarize(versioned+'\n[PersonalKnox] Chatter error: failure'),'0.4.6','0.4.6')
assert not startup_matches(summarize(versioned+'\n[PersonalKnox] Java core 0.4.6 loaded'),'0.4.6','0.4.6'), 'Old Lua must not validate a later core startup'
root=Path(__file__).resolve().parents[1]
source=(root/'common/media/lua/client/PersonalKnox/Client.lua').read_text()
declared=re.search(r'^modversion=(.+)$',(root/'42/mod.info').read_text(),re.M).group(1)
marker=re.search(r'\[PersonalKnox\] Lua addon ([\d.]+) loaded',source).group(1)
assert marker==declared, f'Lua startup marker {marker} differs from mod.info {declared}'
with tempfile.TemporaryDirectory(prefix='personalknox-run-check-') as tmp:
    console=Path(tmp)/'console.txt'
    console.write_text(versioned)
    checker=str(Path(__file__).with_name('check_run.py'))
    for lua_version,expected_exit in [('0.4.6',0),('0.4.53',1)]:
        result=subprocess.run([sys.executable,checker,'--console',str(console),'--expect','0.4.6',
            '--expect-lua',lua_version],capture_output=True,text=True)
        assert result.returncode==expected_exit, result.stdout+result.stderr
print('Run-report checks passed: Java/Lua versions, unversioned/mismatched Lua rejection, explicit mixed releases, CLI exit codes and errors. World behavior is not validated.')
