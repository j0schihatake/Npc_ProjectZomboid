"""Check dev-launch isolation and command construction using temporary files and a fake process."""
from contextlib import redirect_stdout
import io
import json
from pathlib import Path
import tempfile
from unittest.mock import patch

import dev_launch as launch


def mod(root,identifier,requires=''):
    (root/'42').mkdir(parents=True)
    (root/'42/mod.info').write_text(f'id = {identifier}\nrequire={requires}\n')
    return root


with tempfile.TemporaryDirectory(prefix='pk-dev-launch-') as tmp:
    base=Path(tmp);userdata=base/'normal';cache=base/'isolated';game=base/'game'
    source=mod(base/'worktree','PersonalKnox','\\ZombieBuddy,\\KnoxEventExpandedNpc')
    jar=source/'42/media/java/client/PersonalKnox.jar';jar.parent.mkdir(parents=True);jar.write_bytes(b'built fixture jar')
    workshop=base/'workshop'
    for i,identifier in enumerate(launch.MINIMAL[:-1]):
        mod(workshop/str(i)/'mods'/identifier,identifier,'\\KnoxEventExpandedNpc' if identifier.endswith('RU') else '')
    (userdata/'mods').mkdir(parents=True);(userdata/'Lua').mkdir()
    original='VERSION = 1,\nmods\n{\n    mod = PersonalKnox,\n}\nmaps\n{\n    map = MyMap,\n}\n'
    (userdata/'mods/default.txt').write_text(original)
    (userdata/'options.ini').write_text('language=RU\n')
    (userdata/'Lua/ModOptions.ini').write_text('my dev defaults\n')
    (userdata/'Lua/PersonalKnox-probe.txt').write_text('must not be copied')
    (game/'projectzomboid').mkdir(parents=True);(game/'projectzomboid/ProjectZomboid64').write_bytes(b'fixture executable')
    args=['launch_dev','--menu-only','--source',str(source),'--userdata',str(userdata),'--cache-dir',str(cache),
          '--game-root',str(game),'--workshop',str(workshop)]
    def invoke(extra,processes=None,popen=None):
        with patch('sys.argv',args+extra),patch.object(launch,'game_processes',return_value=processes or []),redirect_stdout(io.StringIO()):
            if popen is None:
                return launch.main()
            with patch.object(launch.subprocess,'Popen',popen):
                return launch.main()
    assert invoke(['--dry-run','--approve-own'])==0 and not cache.exists(),'Dry run including trust aliases must not write'
    assert invoke(['--prepare-only'])==0
    assert (cache/'mods/reset-mods-42_00.txt').is_file(),'First B42 startup must not clear the prepared selection'
    assert (userdata/'mods/default.txt').read_text()==original
    assert not (cache/'Saves').exists() and not (cache/'Lua/PersonalKnox-probe.txt').exists()
    assert (cache/'options.ini').read_text()=='language=RU\n'
    prepared=(cache/'mods/default.txt').read_text()
    assert all('mod = '+identifier+',' in prepared for identifier in launch.MINIMAL)
    assert 'MyMap' not in prepared,'Minimal profile must not enable expansion maps'
    marker=json.loads((cache/'.personalknox-dev.json').read_text())
    assert len(marker['links'])==5 and all((cache/'mods'/name).is_symlink() for name in marker['links'])
    assert str(source) in marker['links'].values(),'Load the agent worktree, not a normal installed copy'
    (cache/'options.ini').write_text('my edited dev settings\n')
    invoke(['--prepare-only','--profile','full'])
    assert 'map = MyMap,' in (cache/'mods/default.txt').read_text()
    assert (cache/'options.ini').read_text()=='my edited dev settings\n'
    target_before=(cache/'mods/default.txt').read_bytes()
    try:
        invoke(['--prepare-only'],[(123,['ProjectZomboid64','-cachedir='+str(cache)])])
        raise AssertionError('Preparation modified an active dev cache')
    except ValueError as error:
        assert 'used by a running game' in str(error)
    assert (cache/'mods/default.txt').read_bytes()==target_before
    try:
        invoke([],[(123,['ProjectZomboid64'])])
        raise AssertionError('Duplicate game launch accepted')
    except ValueError as error:
        assert 'already running' in str(error)
    class Process:
        pid=424242
        def wait(self):return 7
    captured={}
    def popen(command,**kwargs):
        captured.update(command=command,**kwargs);return Process()
    assert invoke(['--nosteam'],popen=popen)==7,'Return the game exit code in foreground mode'
    assert captured['command']==[str(game/'projectzomboid/ProjectZomboid64'),'-debug','-cachedir='+str(cache),'-nosteam']
    assert captured['cwd']==game/'projectzomboid' and captured['env']['XMODIFIERS']==''
    assert captured['env']['PATH'].startswith(str(game/'projectzomboid/jre64/bin'))
    state=json.loads((cache/'dev-launch.json').read_text())
    assert state['returncode']==7 and state['pid']==424242
    assert invoke(['--detach'],popen=popen)==0 and captured['start_new_session'] is True
    assert invoke(['--no-debug','--detach'],popen=popen)==0
    assert '-debug' not in captured['command'],'Preserve the agent no-debug option'
    scenario={'nonce':'a'*32,'world':'PersonalKnox-scenario-'+'a'*32,'source':str(source)}
    scenario_args=[arg for arg in args if arg!='--menu-only']+['--detach']
    with patch('sys.argv',scenario_args),patch.object(launch,'game_processes',return_value=[]),patch.object(launch.subprocess,'Popen',popen),patch.object(launch,'choose_save',return_value=source),patch.object(launch,'prepare_scenario',return_value=scenario),patch.object(launch,'build_agent',return_value=base/'agent.jar'),patch.object(launch,'wait_scenario',return_value=0) as wait,redirect_stdout(io.StringIO()):
        assert launch.main()==0
        wait.assert_called_once()
    assert '-javaagent:' in captured['env']['JAVA_TOOL_OPTIONS']
    assert json.loads((cache/'dev-launch.json').read_text())['scenario']['nonce']==scenario['nonce']
    assert (cache/'mods/default.txt').read_text().count('mod = PersonalKnoxDevScenario,')==1

    with patch.object(launch,'restart_owned') as restart:
        assert invoke(['--restart','--detach'],popen=popen)==0
        restart.assert_called_once_with(cache)
    assert (userdata/'mods/default.txt').read_text()==original
    with patch('sys.argv',args+['--cache-dir',str(userdata),'--dry-run']):
        try:
            launch.main();raise AssertionError('Normal userdata accepted as a dev cache')
        except ValueError:
            pass
    unmanaged=base/'unmanaged';unmanaged.mkdir();(unmanaged/'keep.txt').write_text('keep')
    with patch('sys.argv',args+['--cache-dir',str(unmanaged),'--prepare-only']),patch.object(launch,'game_processes',return_value=[]):
        try:
            launch.main();raise AssertionError('Unmanaged data overwritten')
        except ValueError:
            pass
    assert (unmanaged/'keep.txt').read_text()=='keep'
    with patch.object(launch,'game_processes',return_value=[(999,['ProjectZomboid64','-cachedir=/another/cache'])]):
        try:
            launch.restart_owned(cache)
            raise AssertionError('Unowned game accepted for restart')
        except ValueError:
            pass
print('Dev launcher checks passed: isolated profile/settings, dependency links, no save/probe copying, full map order, running-game/cache guards, debug/native environment, detach and exit status. No real game was launched.')
