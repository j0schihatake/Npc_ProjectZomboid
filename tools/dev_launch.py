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
from dev_trust import policy as trust_policy, approve as approve_projects

ROOT = Path(__file__).resolve().parents[1]
GAME = Path('/home/j0schi/SteamLibrary/steamapps/common/ProjectZomboid')
MINIMAL = ['ZombieBuddy','Viewpoint','KnoxEventExpandedNpc','KnoxEventExpandedNpcRU','PersonalKnox']
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
    jar=info.get('javaJarFile')
    return {'id':info['id'],'root':root.resolve(),
            'java_jar':(selected/jar.lstrip('\\')) if jar else None,
            'requires':[x.strip().lstrip('\\') for x in info.get('require','').split(',') if x.strip()]}


def discover(source, userdata, workshop):
    index={}
    for priority,roots in [(0,sorted(workshop.glob('*/mods/*'))),
                           (1,sorted((userdata/'mods').glob('*'))),(2,[source])]:
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
    atomic(cache/'mods/default.txt',config)
    # Seed graphics/language/options once. Never copy arm files or real saves.
    for relative in ['options.ini','Lua/ModOptions.ini']:
        origin=userdata/relative;target=cache/relative
        if origin.is_file() and not target.exists():
            shutil.copy2(origin,target)
    atomic(marker,json.dumps({'kind':MARKER,'source':str(source),'mods':ids,'links':links},ensure_ascii=False,indent=2)+'\n')


def approve_own(source):
    jar=source/'42/media/java/client/PersonalKnox.jar'
    digest=hashlib.sha256(jar.read_bytes()).hexdigest()
    path=Path.home()/'.zombie_buddy/mod_approvals.json'
    data=json.loads(path.read_text()) if path.is_file() else {'formatVersion':2,'mods':[]}
    found=False
    for mod in data.setdefault('mods',[]):
        if mod.get('id')=='PersonalKnox':
            mod['jar_hash']=digest;mod['decision']=True;found=True
    if not found:
        data['mods'].append({'id':'PersonalKnox','jar_hash':digest,'decision':True})
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
    print('Approved PersonalKnox JAR '+digest[:12]+' for ZombieBuddy',flush=True)

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--profile',choices=['minimal','full'],default='minimal')
    parser.add_argument('--source',type=Path,default=ROOT,help='Mod worktree containing 42/mod.info and the built JAR')
    parser.add_argument('--cache-dir',type=Path,default=Path.home()/'Zomboid-dev/PersonalKnox')
    parser.add_argument('--userdata',type=Path,default=Path.home()/'Zomboid',help='Read-only source of settings and the full mod profile')
    parser.add_argument('--game-root',type=Path,default=GAME)
    parser.add_argument('--workshop',type=Path)
    parser.add_argument('--add-mod',action='append',default=[])
    parser.add_argument('--nosteam',action='store_true',help='Installed Workshop mods remain available through local links')
    parser.add_argument('--detach',action='store_true',help='Return PID immediately; no launch timeout or automatic process termination')
    parser.add_argument('--prepare-only',action='store_true')
    parser.add_argument('--dry-run',action='store_true',help='Resolve mods and print the plan without changing files or launching')
    parser.add_argument('--approve-own',action='store_true',help='Alias for --trust-local-builds')
    parser.add_argument('--trust-local-builds',action='store_true',help='Authorize standing trust for the two local project roots')
    parser.add_argument('--approval-store',type=Path,default=Path.home()/'.zombie_buddy/mod_approvals.json')
    args=parser.parse_args()
    source=args.source.expanduser().resolve();userdata=args.userdata.expanduser().resolve()
    cache=args.cache_dir.expanduser().resolve();game=args.game_root.expanduser().resolve()
    if cache==userdata or cache.is_relative_to(userdata) or userdata.is_relative_to(cache):
        raise ValueError('Use a dev cache outside the normal userdata tree')
    if not source.is_dir() or not (source/'42/media/java/client/PersonalKnox.jar').is_file():
        raise ValueError('The source worktree needs a built 42/media/java/client/PersonalKnox.jar')
    own=mod_metadata(source)
    if not own or own['id']!='PersonalKnox':
        raise ValueError('--source must declare id=PersonalKnox for B42.21')
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
    else:
        requested=MINIMAL.copy()
    ids,mods=resolve(discover(source,userdata,workshop),requested+['PersonalKnox']+args.add_mod)
    if args.approve_own or args.trust_local_builds:
        trusted = trust_policy(cache, {'PersonalKnox': source}, True)
        changed = approve_projects(mods, trusted, args.approval_store, cache/'approval-backups')
        if changed: print('Approved local build for ZombieBuddy: ' + ', '.join(changed), flush=True)

    command=[str(binary),'-debug','-cachedir='+str(cache)]+(['-nosteam'] if args.nosteam else [])
    if args.dry_run:
        print(json.dumps({'command':command,'cwd':str(engine),'cache':str(cache),'source':str(source),
                          'mods':ids,'mod_roots':{key:str(value['root']) for key,value in mods.items()}},ensure_ascii=False,indent=2))
        return 0
    if not args.prepare_only and game_processes():
        raise ValueError('A game is already running; close it first. This launcher does not stop processes.')
    # The lock lives outside the initially empty cache so ownership checks stay meaningful.
    cache.parent.mkdir(parents=True,exist_ok=True)
    with (cache.parent/(cache.name+'.launch.lock')).open('a') as lock:
        try:
            fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
        except BlockingIOError:
            raise ValueError('Another launcher is preparing or running this dev cache') from None
        prepare(cache,userdata,source,ids,mods,maps)
        print('Dev cache: '+str(cache),flush=True)
        print('Mods: '+', '.join(ids),flush=True)
        print('Command: '+shlex.join(command),flush=True)
        if args.prepare_only:
            return 0
        if game_processes():
            raise ValueError('A game started during preparation; launch skipped')
        env=os.environ.copy()
        env['PATH']=str(engine/'jre64/bin')+os.pathsep+env.get('PATH','')
        env['LD_LIBRARY_PATH']=':'.join([str(engine/'natives'),str(engine),str(engine/'jre64/lib')]+([env['LD_LIBRARY_PATH']] if env.get('LD_LIBRARY_PATH') else []))
        env['LD_PRELOAD']=':'.join(([env['LD_PRELOAD']] if env.get('LD_PRELOAD') else [])+['libjsig.so','libPZXInitThreads64.so'])
        env['XMODIFIERS']=''
        logs=cache/'launch-logs';logs.mkdir(exist_ok=True)
        log=logs/(datetime.now().strftime('%Y%m%d-%H%M%S-%f')+'.log')
        with log.open('wb') as output:
            process=subprocess.Popen(command,cwd=engine,env=env,stdin=subprocess.DEVNULL,
                                     stdout=output,stderr=subprocess.STDOUT,start_new_session=args.detach)
        state={'pid':process.pid,'started':datetime.now().isoformat(),'command':command,'source':str(source),
               'profile':args.profile,'mods':ids,'launcher_log':str(log),'console':str(cache/'console.txt')}
        atomic(cache/'dev-launch.json',json.dumps(state,ensure_ascii=False,indent=2)+'\n')
        print(f'PID: {process.pid}\nConsole: {cache/"console.txt"}\nLauncher log: {log}',flush=True)
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
