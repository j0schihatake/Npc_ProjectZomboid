"""Build the private Java addon locally, with no upload or dependency download."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
AGENT_DIR = Path('/home/j0schi/SteamLibrary/steamapps/common/ProjectZomboid/projectzomboid')
DEFAULT_AGENT = AGENT_DIR / 'ZombieBuddy.jar'
DEFAULT_GAME = AGENT_DIR / 'projectzomboid.jar'

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--agent', type=Path, default=Path(os.environ.get('ZOMBIE_BUDDY_JAR', DEFAULT_AGENT)))
    args = parser.parse_args()
    if not args.agent.is_file():
        raise SystemExit('Pass --agent /path/to/ZombieBuddy.jar')
    subprocess.run([sys.executable, str(ROOT/'tools/export_dialogues.py')], check=True)
    classes = ROOT/'build/classes'
    if classes.exists(): shutil.rmtree(classes)
    classes.mkdir(parents=True)
    sources = sorted((ROOT/'src/main/java').rglob('*.java'))
    subprocess.run(['javac', '--release', '17', '-encoding', 'UTF-8', '-cp', str(args.agent)+os.pathsep+str(DEFAULT_GAME), '-d', str(classes), *map(str,sources)], check=True)
    shutil.copytree(ROOT/'src/main/resources', classes, dirs_exist_ok=True)
    tests = ROOT/'build/test-classes'
    tests.mkdir(parents=True, exist_ok=True)
    classpath = os.pathsep.join([str(classes), str(args.agent), str(DEFAULT_GAME)])
    test_sources=sorted((ROOT/'src/test/java').rglob('*.java'))
    subprocess.run(['javac', '--release', '17', '-encoding', 'UTF-8', '-cp', classpath, '-d', str(tests), *map(str,test_sources)], check=True)
    subprocess.run(['java', '-ea', '-cp', classpath+os.pathsep+str(tests), 'personal.knox.BrainTest'], check=True)
    jar = ROOT/'42/media/java/client/PersonalKnox.jar'
    jar.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(['jar', '--create', '--date=2020-01-01T00:00:00Z', '--file', str(jar), '-C', str(classes), '.'], check=True)
    print('Built:', jar)
    try:
        from approval_store import approve
        if approve('PersonalKnox', jar, ROOT/'build'/'approval-backups'):
            print('Approved PersonalKnox JAR in ZombieBuddy store')
    except Exception as error:
        print('Approval store update skipped:', error)


if __name__ == '__main__': main()
