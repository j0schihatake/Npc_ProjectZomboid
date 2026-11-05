"""Check save-copy identity, profile overrides, cleanup and refusal paths in temporary userdata."""
import json
from pathlib import Path
import sqlite3
import tempfile
from unittest.mock import patch

import arm_probe
import prepare_probe_world

with tempfile.TemporaryDirectory(prefix='personalknox-arm-check-') as tmp:
    userdata=Path(tmp)
    source=userdata/'Saves/Sandbox/Original'
    source.mkdir(parents=True)
    db=sqlite3.connect(source/'players.db');db.execute('create table test (value integer)');db.commit();db.close()
    (source/'mods.txt').write_text('source profile must remain untouched\n')
    (source/'map.bin').write_bytes(b'original map data')
    installed=userdata/'mods/PersonalKnox/common/media/lua/client/PersonalKnox'
    installed.mkdir(parents=True)
    (installed/'Probe.lua').write_bytes((arm_probe.ROOT/'common/media/lua/client/PersonalKnox/Probe.lua').read_bytes())
    (installed/'Client.lua').write_text('Probe.install({})')
    profile=userdata/'mods/default.txt'
    profile.write_text('VERSION = 1,\nmods\n{\n'+''.join(f'    mod = {name},\n' for name in ['ZombieBuddy','KnoxEventExpandedNpc','Viewpoint','PersonalKnox'])+'}\n')
    profile_before=profile.read_bytes()
    latest=userdata/'latestSave.ini';latest.write_bytes(b'Original\r\nSandbox\r\n')
    latest_before=latest.read_bytes()
    original=prepare_probe_world.fingerprint(source)
    # Only temporary fixture userdata is used; never disable a real-process guard in production.
    with patch.object(arm_probe,'require_closed_game'),patch.object(prepare_probe_world,'require_closed_game'):
        record_path=arm_probe.arm(userdata,'Original',10,True)
        record=json.loads(record_path.read_text());copy=Path(record['copy'])
        assert copy!=source and copy.name.startswith('PersonalKnox-test-')
        assert prepare_probe_world.fingerprint(source)==original
        assert (copy/'mods.txt').read_bytes()==profile_before and profile.read_bytes()==profile_before
        assert latest.read_bytes()==latest_before
        control=userdata/'Lua/PersonalKnox-probe.txt'
        assert control.read_text().splitlines()[1:4]==[copy.name,record['nonce'],'armed']
        assert (userdata/'Lua/PersonalKnox-probes'/f'{copy.name}.txt').read_text().strip()==record['nonce']
        try:
            arm_probe.arm(userdata,'Original')
            raise AssertionError('Must not overwrite an armed run')
        except ValueError:
            pass
        latest.write_text(copy.name+'\nSandbox\n')
        unchanged,restored=arm_probe.cleanup(userdata,record_path)
        assert unchanged and restored and latest.read_bytes()==latest_before and not control.exists()
        assert copy.is_dir() and profile.read_bytes()==profile_before
        latest.write_text('UserChosen\nSandbox\n')
        unchanged,restored=arm_probe.cleanup(userdata,record_path)
        assert unchanged and not restored and latest.read_text()=='UserChosen\nSandbox\n'
        # A concurrent profile edit must leave the new copy unarmed.
        prepare=arm_probe.prepare
        def changed_profile(*args):
            result=prepare(*args)
            profile.write_bytes(profile_before+b'changed during copying\n')
            return result
        with patch.object(arm_probe,'prepare',side_effect=changed_profile):
            try:
                arm_probe.arm(userdata,'Original')
                raise AssertionError('Profile race accepted')
            except RuntimeError as error:
                assert 'profile changed' in str(error)
        assert not control.exists() and prepare_probe_world.fingerprint(source)==original
    before=sorted(p.name for p in (userdata/'Saves/Sandbox').iterdir())
    with patch.object(arm_probe,'require_closed_game',side_effect=RuntimeError('Game is live')):
        try:
            arm_probe.arm(userdata,'Original')
            raise AssertionError('Live game accepted')
        except RuntimeError as error:
            assert str(error)=='Game is live'
    assert before==sorted(p.name for p in (userdata/'Saves/Sandbox').iterdir())
print('Probe preparation passed: copied-world identity, unchanged source/default/latest pointer, copied current profile, conditional pointer restoration, profile race and live-game refusal.')
