"""Prepare/launch an isolated B42.21 dev profile for PersonalKnox; never stop another game."""
import argparse
from datetime import datetime
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import sys
import signal
import select
import time
from dev_trust import policy as trust_policy, approve as approve_projects
from dev_scenario import choose_save, prepare_scenario, wait_scenario, build_agent, runtime_errors

ROOT = Path(__file__).resolve().parents[1]
GAME = Path('/home/j0schi/SteamLibrary/steamapps/common/ProjectZomboid')
MINIMAL = ['ZombieBuddy','Viewpoint','PersonalKnox']
MARKER = 'PersonalKnoxDevLauncher1'


def mod_metadata(root):
    versions=[]
    for path in root.iterdir():
        if path.is_dir() and re.fullmatch(r'42(?:\.\d+)*',path.name):
            version=tuple(map(int,path.name.split('.')))
            if version <= (42,21,0):
                versions.append((version,path))
    if not versions:
        return None
    selected=max(versions)[1]
    info={}
    for path in (root/'common/mod.info',selected/'mod.info'):
        if path.is_file():
            info.update((key.strip(),value.strip()) for key,value in re.findall(
                r'^([^=\r\n]+)\s*=\s*(.*?)\s*$',path.read_text(errors='replace'),re.M))
    if not info.get('id'):
        return None
    return {'id':info['id'],'root':root.resolve(),'version_root':selected,
            'java_jar':(selected/info['javaJarFile'].lstrip('\\')) if info.get('javaJarFile') else None,
            'requires':[x.strip().lstrip('\\') for x in info.get('require','').split(',') if x.strip()]}


def discover(source, userdata, workshop, worldgen=None):
    index={}
    for priority,roots in [(0,sorted(workshop.glob('*/mods/*'))),
                           (1,sorted((userdata/'mods').glob('*'))),(2,[source]+([worldgen] if worldgen else []))]:
        for root in roots:
            if not root.is_dir():
                continue
            info=mod_metadata(root)
            if info:
                entry=index.setdefault(info['id'],[])
                entry.append((priority,info))
    return index


def resolve(index, requested):
    ordered=[];selected={};visiting=set()
    def visit(identifier):
        if identifier in visiting:
            raise ValueError('Cyclic mod dependency: '+identifier)
        if identifier in selected:
            return
        candidates=index.get(identifier,[])
        if not candidates:
            raise ValueError('Installed B42.21 mod not found: '+identifier)
        priority=max(p for p,_ in candidates)
        choices={str(m['root']):m for p,m in candidates if p==priority}
        if len(choices)!=1:
            raise ValueError('Ambiguous installed mod '+identifier+': '+', '.join(choices))
        info=next(iter(choices.values()))
        visiting.add(identifier)
        for required in info['requires']:
            visit(required)
        visiting.remove(identifier)
        selected[identifier]=info;ordered.append(identifier)
    for identifier in requested:
        visit(identifier)
    return ordered,selected


def game_processes():
    result=[]
    for proc in Path('/proc').iterdir():
        if not proc.name.isdigit():
            continue
        try:
            args=(proc/'cmdline').read_bytes().decode(errors='replace').split('\0')
        except (FileNotFoundError,ProcessLookupError,PermissionError):
            continue
        if args and Path(args[0]).name=='ProjectZomboid64':
            result.append((int(proc.name),args))
    return result


def atomic(path, data):
    temporary=path.with_name(path.name+'.dev-tmp')
    temporary.write_text(data,encoding='utf-8')
    os.replace(temporary,path)


def process_start(pid):
    try:
        return (Path('/proc')/str(pid)/'stat').read_text().rsplit(')',1)[1].split()[19]
    except (FileNotFoundError,ProcessLookupError):
        return None


def restart_owned(cache, timeout=90):
    games=game_processes()
    if not games:
        return
    state_path=cache/'dev-launch.json'
    if not state_path.is_file():
        raise ValueError('No owned dev-launch record; close the existing game manually')
    state=json.loads(state_path.read_text())
    matches=[pid for pid,args in games if pid==state.get('pid') and '-cachedir='+str(cache) in args]
    if len(games)!=1 or len(matches)!=1:
        raise ValueError('Restart refused: the live game is not exclusively this launcher-owned dev instance')
    pid=matches[0]
    fd=os.pidfd_open(pid)
    try:
        if state.get('process_start') and process_start(pid)!=state['process_start']:
            raise ValueError('Restart refused: the recorded PID has been reused')
        print('Stopping owned dev PID '+str(pid)+' with SIGTERM; waiting for exit...',flush=True)
        signal.pidfd_send_signal(fd,signal.SIGTERM)
        poll=select.poll();poll.register(fd,select.POLLIN)
        deadline=time.monotonic()+timeout
        while not poll.poll(500):
            if time.monotonic()>=deadline:
                raise ValueError('Dev process did not exit; no forced kill and no second launch performed')
    finally:
        os.close(fd)


def show_status(cache):
    path=cache/'dev-launch.json'
    state=json.loads(path.read_text()) if path.exists() else {}
    games=game_processes()
    live=any(pid==state.get('pid') and '-cachedir='+str(cache) in args and
             (not state.get('process_start') or process_start(pid)==state['process_start']) for pid,args in games)
    console=cache/'console.txt'
    started_ns=state.get('started_ns',int(datetime.fromisoformat(state['started']).timestamp()*1e9) if state.get('started') else 0)
    current_log=console.exists() and console.stat().st_mtime_ns>=started_ns
    text=console.read_text(errors='replace') if current_log else ''
    counts=re.findall(r'loadMods\((\d+) mods\)',text)
    core=re.findall(r'\[PersonalKnox\] Java core ([\d.]+) loaded',text)
    lua=re.findall(r'\[PersonalKnox\] Lua addon (\S+) loaded',text)
    phase='not_running'
    scenario=None
    if live:
        phase='starting'
        if text.rfind('Waiting for Java mods approval')>text.rfind('Java mod approvals written'):
            phase='waiting_for_java_approval'
        elif counts and counts[-1]=='0':
            phase='no_mods_loaded'
        elif lua:
            phase='mods_loaded_world_unverified'
        if any(int(frame)>0 for frame in re.findall(r'\bf:(\d+)>',text)):
            phase='world_ticks_seen'
    if state.get('scenario'):
        try:
            result=json.loads((cache/'Lua/PersonalKnox-dev-scenario-result.json').read_text())
            if result.get('nonce')==state['scenario']['nonce'] and result.get('world')==state['scenario']['world']:
                scenario=result
                phase='scenario_'+result.get('status','unknown').lower() if live else phase
                errors=runtime_errors(text,state['scenario']['nonce'])
                if errors:
                    scenario['runtime_errors']=errors
                    if live: phase='scenario_failed'
                elif state.get('scenario_verification',{}).get('exit_code',0)!=0 and live:
                    phase='scenario_failed'
        except (FileNotFoundError,json.JSONDecodeError):
            pass
    print(json.dumps({'live':live,'pid':state.get('pid'),'phase':phase,'loaded_mods':int(counts[-1]) if counts else None,
                      'core':core[-1] if core else None,'lua':lua[-1] if lua else None,
                      'console':str(console),'log_is_current':current_log,'launcher_log':state.get('launcher_log'),
                      'scenario':scenario},ensure_ascii=False,indent=2))


def prepare(cache, userdata, source, ids, mods, maps):
    marker=cache/'.personalknox-dev.json'
    if cache.exists() and any(cache.iterdir()) and not marker.is_file():
        raise ValueError('Refusing to change a nonempty unmanaged cache: '+str(cache))
    if marker.exists() and json.loads(marker.read_text()).get('kind')!=MARKER:
        raise ValueError('Unknown dev-cache marker: '+str(marker))
    # A detached launcher no longer owns the lock: also inspect the actual game argv.
    if any('-cachedir='+str(cache) in args for _,args in game_processes()):
        raise ValueError('This dev cache is used by a running game')
    cache.mkdir(parents=True,exist_ok=True)
    (cache/'mods').mkdir(exist_ok=True);(cache/'Lua').mkdir(exist_ok=True)
    old=json.loads(marker.read_text()) if marker.exists() else {'links':{}}
    if any(Path(name).name!=name or not name.startswith('_pkdev_') for name in old.get('links',{})):
        raise ValueError('Invalid managed link name in the dev-cache marker')
    links={}
    for root in sorted({str(m['root']) for m in mods.values()}):
        name='_pkdev_'+Path(root).name+'-'+hashlib.sha256(root.encode()).hexdigest()[:8]
        link=cache/'mods'/name
        if link.exists() or link.is_symlink():
            if not link.is_symlink() or str(link.resolve())!=root:
                raise ValueError('Dev mod-link collision: '+str(link))
        links[name]=root
    for name,target in links.items():
        link=cache/'mods'/name
        if not link.is_symlink():
            link.symlink_to(target,target_is_directory=True)
    for name,target in old.get('links',{}).items():
        link=cache/'mods'/name
        if name not in links and link.is_symlink() and str(link.resolve())==target:
            link.unlink()
    config='VERSION = 1,\n\nmods\n{\n'+''.join('    mod = '+identifier+',\n' for identifier in ids)+'}\n\n'+maps+'\n'
    # B42 clears default.txt once when this marker is absent, even when the
    # launcher has just explicitly prepared a compatible B42 dev profile.
    # Initialize only the B42 marker; future major-version resets remain intact.
    reset_marker=cache/'mods/reset-mods-42_00.txt'
    if not reset_marker.exists():
        atomic(reset_marker,'If this file does not exist, default.txt will be reset to empty (no mods active).')
    atomic(cache/'mods/default.txt',config)
    # Seed graphics/language/options once. Never copy arm files or real saves.
    for relative in ['options.ini','Lua/ModOptions.ini']:
        origin=userdata/relative;target=cache/relative
        if origin.is_file() and not target.exists():
            shutil.copy2(origin,target)
    atomic(marker,json.dumps({'kind':MARKER,'source':str(source),'mods':ids,'links':links},ensure_ascii=False,indent=2)+'\n')


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--launcher-protocol',action='store_true',help=argparse.SUPPRESS)
    parser.add_argument('--profile',choices=['minimal','full','worldgen','both'],default='minimal')
    parser.add_argument('--source',type=Path,default=ROOT,help='Mod worktree containing 42/mod.info and the built JAR')
    parser.add_argument('--worldgen-source',type=Path,default=ROOT.parent/'PersonalWorldGen-dev')
    parser.add_argument('--trust-local-builds',action='store_true',help='Authorize standing trust for the two explicit local project roots')
    parser.add_argument('--approval-store',type=Path,default=Path.home()/'.zombie_buddy/mod_approvals.json')
    parser.add_argument('--cache-dir',type=Path,default=Path.home()/'Zomboid-dev/PersonalKnox')
    parser.add_argument('--userdata',type=Path,default=Path.home()/'Zomboid',help='Read-only source of settings and the full mod profile')
    parser.add_argument('--game-root',type=Path,default=GAME)
    parser.add_argument('--workshop',type=Path)
    parser.add_argument('--add-mod',action='append',default=[])
    parser.add_argument('--no-debug',action='store_true',help='Launch without -debug')
    parser.add_argument('--menu-only',action='store_true',help='Only open the main menu; skip the automatic world scenario')
    parser.add_argument('--approve-own',action='store_true',help='Compatibility alias for --trust-local-builds')
    parser.add_argument('--nosteam',action='store_true',help='Installed Workshop mods remain available through local links')
    parser.add_argument('--detach',action='store_true',help='Detach the game; still wait for scenario OK unless --menu-only is selected')
    parser.add_argument('--restart',action='store_true',help='SIGTERM only this recorded dev process, wait for exit, then launch again')
    parser.add_argument('--build',action='store_true',help='Run the selected project build scripts after stopping the old dev process')
    parser.add_argument('--status',action='store_true',help='Read the actual dev PID and loading phase without changing anything')
    parser.add_argument('--scenario',choices=['smoke','own-npc','own-restore'],help='Load a disposable Sandbox copy and wait for a verified OK/FAIL result')
    parser.add_argument('--scenario-save',type=Path,help='Closed Sandbox save to copy; defaults to normal latestSave.ini')
    parser.add_argument('--scenario-seconds',type=int,default=10)
    parser.add_argument('--scenario-timeout',type=int,default=600)
    parser.add_argument('--prepare-only',action='store_true')
    parser.add_argument('--dry-run',action='store_true',help='Resolve mods and print the plan without changing files or launching')
    args=parser.parse_args()
    if args.launcher_protocol:
        print('PersonalKnoxDevLauncher2');return 0
    if args.menu_only and args.scenario:
        parser.error('--menu-only and --scenario are mutually exclusive')
    if not args.menu_only and not (args.status or args.prepare_only or args.dry_run):
        args.scenario=args.scenario or 'own-npc'
    args.trust_local_builds=args.trust_local_builds or args.approve_own
    source=args.source.expanduser().resolve();userdata=args.userdata.expanduser().resolve()
    worldgen=args.worldgen_source.expanduser().resolve()
    cache=args.cache_dir.expanduser().resolve();game=args.game_root.expanduser().resolve()
    if cache==userdata or cache.is_relative_to(userdata) or userdata.is_relative_to(cache):
        raise ValueError('Use a dev cache outside the normal userdata tree')
    if args.status:
        show_status(cache);return 0
    if args.profile!='worldgen':
        if not source.is_dir() or (not args.build and not (source/'42/media/java/client/PersonalKnox.jar').is_file()):
            raise ValueError('The source worktree needs a built PersonalKnox.jar or --build')
        own=mod_metadata(source)
        if not own or own['id']!='PersonalKnox':
            raise ValueError('--source must declare id=PersonalKnox for B42.21')
    if args.profile in ('worldgen','both'):
        generator=mod_metadata(worldgen) if worldgen.is_dir() else None
        if not generator or generator['id']!='PersonalWorldGen':
            raise ValueError('PersonalWorldGen has no B42 game package yet (42/mod.info); its development sources remain a separate project')
    engine=game/'projectzomboid';binary=engine/'ProjectZomboid64'
    if not binary.is_file():
        raise ValueError('Game executable not found: '+str(binary))
    workshop=args.workshop.expanduser().resolve() if args.workshop else game.parents[1]/'workshop/content/108600'
    maps='maps\n{\n}'
    if args.profile=='full':
        text=(userdata/'mods/default.txt').read_text()
        requested=re.findall(r'^\s*mod\s*=\s*(.+?)\s*,?\s*$',text,re.M)
        sections=re.findall(r'^maps\s*\{.*?^\}',text,re.M|re.S)
        if not requested or len(sections)!=1:
            raise ValueError('Unknown full-profile format; refusing to guess its map order')
        maps=sections[0]
    elif args.profile=='worldgen':
        requested=['ZombieBuddy','Viewpoint','PersonalWorldGen']
    elif args.profile=='both':
        requested=MINIMAL+['PersonalWorldGen']
    else:
        requested=MINIMAL.copy()
    index=discover(source,userdata,workshop,worldgen)
    scenario_source=None
    if args.scenario:
        if args.prepare_only or args.profile=='worldgen':
            raise ValueError('Scenario requires a real launch with PersonalKnox')
        if not 2<=args.scenario_seconds<=300 or not 10<=args.scenario_timeout<=1800:
            raise ValueError('Scenario seconds must be 2..300 and timeout 10..1800')
        harness=mod_metadata(ROOT/'tools/dev_scenario_mod')
        index[harness['id']]=[(3,harness)]
        requested.append(harness['id'])
    ids,mods=resolve(index,requested+args.add_mod)
    if args.scenario:
        map_roots=[engine/'media/maps']
        for mod in mods.values():
            map_roots.extend([mod['root']/'common/media/maps',mod['version_root']/'media/maps'])
        available_maps={p.name for root in map_roots if root.is_dir() for p in root.iterdir() if p.is_dir()}
        scenario_source=choose_save(userdata,args.scenario_save,available_maps)
        print('Scenario template: '+str(scenario_source),flush=True)
    command=[str(binary)]+([] if args.no_debug else ['-debug'])+['-cachedir='+str(cache)]+(['-nosteam'] if args.nosteam else [])
    if args.dry_run:
        print(json.dumps({'command':command,'cwd':str(engine),'cache':str(cache),'source':str(source),
                          'mods':ids,'mod_roots':{key:str(value['root']) for key,value in mods.items()}},ensure_ascii=False,indent=2))
        return 0
    if not args.prepare_only and not args.restart and game_processes():
        raise ValueError('A game is already running; close it first. This launcher does not stop processes.')
    # The lock lives outside the initially empty cache so ownership checks stay meaningful.
    cache.parent.mkdir(parents=True,exist_ok=True)
    with (cache.parent/(cache.name+'.launch.lock')).open('a') as lock:
        try:
            fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
        except BlockingIOError:
            raise ValueError('Another launcher is preparing or running this dev cache') from None
        if args.restart:
            restart_owned(cache)
        if args.build:
            for identifier,mod in mods.items():
                if identifier not in ('PersonalKnox','PersonalWorldGen'):
                    continue
                script=mod['root']/'tools/build.py'
                if not script.is_file():
                    raise ValueError('Build script not found: '+str(script))
                subprocess.run([sys.executable,str(script)],cwd=mod['root'],check=True)
        prepare(cache,userdata,source,ids,mods,maps)
        scenario_record=prepare_scenario(cache,scenario_source,args.scenario_seconds,atomic) if args.scenario else None
        npc_control=cache/'Lua/PersonalKnox-dev-npc.txt'
        if scenario_record and args.scenario in ('own-npc','own-restore'):
            atomic(npc_control,scenario_record['nonce']+'\n'+args.scenario+'\n')
        else:
            npc_control.unlink(missing_ok=True)
        trusted=trust_policy(cache,{'PersonalKnox':source,'PersonalWorldGen':worldgen},args.trust_local_builds)
        print('Dev cache: '+str(cache),flush=True)
        print('Mods: '+', '.join(ids),flush=True)
        print('Command: '+shlex.join(command),flush=True)
        if args.prepare_only:
            return 0
        if game_processes():
            raise ValueError('A game started during preparation; launch skipped')
        approved=approve_projects(mods,trusted,args.approval_store.expanduser().resolve(),cache/'approval-backups')
        if approved:
            print('Approved current local builds: '+', '.join(approved),flush=True)
        env=os.environ.copy()
        scenario_agent=None
        if scenario_record:
            agent=build_agent(cache,ROOT)
            scenario_agent={'path':str(agent),'sha256':hashlib.sha256(agent.read_bytes()).hexdigest()}
            option='-javaagent:'+str(agent)+'='+str(cache/'Lua/PersonalKnox-dev-scenario.txt')
            env['JAVA_TOOL_OPTIONS']=env.get('JAVA_TOOL_OPTIONS','')+' '+shlex.quote(option)
        env['PATH']=str(engine/'jre64/bin')+os.pathsep+env.get('PATH','')
        env['LD_LIBRARY_PATH']=':'.join([str(engine/'natives'),str(engine),str(engine/'jre64/lib')]+([env['LD_LIBRARY_PATH']] if env.get('LD_LIBRARY_PATH') else []))
        env['LD_PRELOAD']=':'.join(([env['LD_PRELOAD']] if env.get('LD_PRELOAD') else [])+['libjsig.so','libPZXInitThreads64.so'])
        env['XMODIFIERS']=''
        logs=cache/'launch-logs';logs.mkdir(exist_ok=True)
        log=logs/(datetime.now().strftime('%Y%m%d-%H%M%S-%f')+'.log')
        java_jars={str(mod['java_jar'].resolve()):hashlib.sha256(mod['java_jar'].read_bytes()).hexdigest()
                   for mod in mods.values() if mod.get('java_jar') and mod['java_jar'].is_file()}
        if scenario_record:
            scenario_record['java_jars']=java_jars
            scenario_record['scenario_agent']=scenario_agent
            atomic(cache/'dev-scenario.json',json.dumps(scenario_record,ensure_ascii=False,indent=2)+'\n')
        started_ns=time.time_ns()
        with log.open('wb') as output:
            process=subprocess.Popen(command,cwd=engine,env=env,stdin=subprocess.DEVNULL,
                                     stdout=output,stderr=subprocess.STDOUT,start_new_session=args.detach)
        state={'pid':process.pid,'started':datetime.now().isoformat(),'command':command,'source':str(source),
               'started_ns':started_ns,
               'process_start':process_start(process.pid),
               'profile':args.profile,'mods':ids,'launcher_log':str(log),'console':str(cache/'console.txt')}
        if scenario_record:
            state['scenario']={key:scenario_record[key] for key in ('nonce','world','source')}
            state['java_jars']=java_jars
            state['scenario_agent']=scenario_agent
        atomic(cache/'dev-launch.json',json.dumps(state,ensure_ascii=False,indent=2)+'\n')
        print(f'PID: {process.pid}\nConsole: {cache/"console.txt"}\nLauncher log: {log}',flush=True)
        if scenario_record:
            result=wait_scenario(cache,scenario_record,process,args.scenario_timeout)
            state['scenario_verification']={'exit_code':result,'finished':datetime.now().isoformat()}
            atomic(cache/'dev-launch.json',json.dumps(state,ensure_ascii=False,indent=2)+'\n')
            return result
        if args.detach:
            return 0
        result=process.wait()  # No timeout: map/NPC loading may take several minutes.
        state.update(exited=datetime.now().isoformat(),returncode=result)
        atomic(cache/'dev-launch.json',json.dumps(state,ensure_ascii=False,indent=2)+'\n')
        return result


if __name__=='__main__':
    try:
        raise SystemExit(main())
    except (ValueError,OSError) as error:
        print('Dev launch: '+str(error),file=sys.stderr)
        raise SystemExit(1)
