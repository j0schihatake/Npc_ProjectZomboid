"""Copy a closed single-player world for local diagnostics; never launch it."""
import argparse
from datetime import datetime
import hashlib
import json
from pathlib import Path
import shutil
import sqlite3
import subprocess
import tempfile


def require_closed_game():
    result = subprocess.run(
        ['pgrep', '-f', '[P]rojectZomboid64'], capture_output=True, text=True)
    if result.returncode != 1:
        raise RuntimeError('Close Project Zomboid before copying a world')


def fingerprint(root):
    result = {}
    for path in sorted(root.rglob('*')):
        if path.is_symlink():
            raise RuntimeError(f'Symlink in save: {path}')
        if path.is_file():
            digest = hashlib.sha256()
            with path.open('rb') as stream:
                for chunk in iter(lambda: stream.read(1024 * 1024), b''):
                    digest.update(chunk)
            result[str(path.relative_to(root))] = digest.hexdigest()
    return result


def prepare(userdata, source_name):
    require_closed_game()
    if not source_name or Path(source_name).name != source_name or source_name in ('.', '..'):
        raise ValueError('Pass one save directory name, without a path')
    saves = userdata / 'Saves' / 'Sandbox'
    source = saves / source_name
    if source.is_symlink() or not source.is_dir():
        raise ValueError(f'Expected a Sandbox save directory: {source}')
    if not (source / 'players.db').is_file() or not (source / 'mods.txt').is_file():
        raise ValueError('Expected players.db and mods.txt in the source world')
    before = fingerprint(source)
    staging = Path(tempfile.mkdtemp(prefix='.PersonalKnox-copy-', dir=saves))
    stamp = datetime.now().strftime('%Y%m%d-%H%M%S')
    destination = saves / f'PersonalKnox-test-{stamp}-{staging.name[-8:]}'
    try:
        shutil.copytree(source, staging, dirs_exist_ok=True)
        require_closed_game()
        if before != fingerprint(source) or before != fingerprint(staging):
            raise RuntimeError('Source changed during copying, or copy verification failed')
        # Read-only check of the copied player database, never the original database.
        connection = sqlite3.connect((staging / 'players.db').as_uri() + '?mode=ro', uri=True)
        try:
            if connection.execute('PRAGMA quick_check').fetchall() != [('ok',)]:
                raise RuntimeError('Copied players.db did not pass quick_check')
        finally:
            connection.close()
        # SQLite must not have changed copied save bytes during the read-only check.
        if before != fingerprint(staging):
            raise RuntimeError('Database check changed the copy; refusing to publish it')
        manifest = {'source': str(source), 'created': stamp, 'sha256': before,
                    'purpose': 'PersonalKnox diagnostics; independent copy of the world'}
        (staging / 'PersonalKnox-test.json').write_text(
            json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        if destination.exists():
            raise RuntimeError(f'Destination already exists: {destination}')
        staging.rename(destination)
        return destination
    except BaseException:
        shutil.rmtree(staging)
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('save', help='Directory name under Saves/Sandbox')
    parser.add_argument('--userdata', type=Path, default=Path('/home/j0schi/Zomboid'))
    args = parser.parse_args()
    destination = prepare(args.userdata.resolve(), args.save)
    print(f'Prepared and verified: {destination}')
    print('Open this copy through Load. latestSave.ini and mod configuration were not changed.')


if __name__ == '__main__':
    main()
