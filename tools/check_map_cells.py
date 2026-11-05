"""Read enabled local map files; report cell overlaps, not runtime compatibility."""
import argparse
import hashlib
import itertools
import json
from pathlib import Path
import re


def inspect(workshop, config):
    enabled = set(re.findall(r'^\s*mod\s*=\s*(.+?)\s*,?\s*$', config.read_text(), re.M))
    maps = []
    found = set()
    lua_files, tile_files = {}, {}
    for root in sorted(workshop.glob('*/mods/*')):
        if not root.is_dir():
            continue
        versions = []
        for path in root.iterdir():
            if path.is_dir() and re.fullmatch(r'42(?:\.\d+)*', path.name):
                version = tuple(int(v) for v in path.name.split('.'))
                if version <= (42, 21, 0):
                    versions.append((version, path))
        if not versions:
            continue
        selected = max(versions)[1]
        infos = [p for p in (root / 'common' / 'mod.info', selected / 'mod.info') if p.is_file()]
        identifiers = set()
        for info in infos:
            identifiers.update(re.findall(r'^id\s*=\s*(.+?)\s*$', info.read_text(errors='replace'), re.M))
        active = identifiers & enabled
        if not active:
            continue
        metadata = {}
        for info in infos:
            metadata.update(re.findall(r'^([^=\r\n]+)=(.*)$', info.read_text(errors='replace'), re.M))
        requirements = [name.strip().lstrip('\\') for name in metadata.get('require', '').split(',') if name.strip()]
        # Resolve each mod's layers before comparing paths across mods.
        effective_lua, definitions = {}, {}
        for layer in (root / 'common', selected):
            for file in (layer / 'media/lua').rglob('*.lua'):
                effective_lua[str(file.relative_to(layer))] = file
            info = layer / 'mod.info'
            if info.is_file():
                for pack, number in re.findall(r'^tiledef=(\S+)\s+(\d+)\s*$', info.read_text(errors='replace'), re.M):
                    definitions[int(number)] = pack
        for path, file in effective_lua.items():
            lua_files.setdefault(path, []).append({'mods': sorted(active), 'sha256': hashlib.sha256(file.read_bytes()).hexdigest()})
        for number, pack in definitions.items():
            files = [path for path in (selected / 'media' / (pack + '.tiles'), root / 'common/media' / (pack + '.tiles')) if path.is_file()]
            digest = hashlib.sha256(files[0].read_bytes()).hexdigest() if files else 'missing'
            tile_files.setdefault(number, []).append({'mods': sorted(active), 'pack': pack, 'sha256': digest})
        found.update(active)
        folders = {}
        for layer in (root / 'common', selected):
            directory = layer / 'media/maps'
            if not directory.is_dir():
                continue
            for folder in directory.iterdir():
                if not folder.is_dir():
                    continue
                entry = folders.setdefault(folder.name, {'headers': set(), 'lots': ''})
                for header in folder.glob('*.lotheader'):
                    match = re.fullmatch(r'(-?\d+)_(-?\d+)\.lotheader', header.name)
                    if match:
                        entry['headers'].add(tuple(map(int, match.groups())))
                info = folder / 'map.info'
                if info.is_file():
                    lots = re.search(r'^lots\s*=\s*(.*?)\s*$', info.read_text(errors='replace'), re.M)
                    entry['lots'] = lots[1] if lots else ''
        for folder, data in sorted(folders.items()):
            if data['headers']:
                maps.append({'mods': sorted(active), 'folder': folder, 'lots': data['lots'],
                             'versionFolder': selected.name, 'workshop': root.parent.parent.name,
                             'requirements': requirements,
                             'missingEnabledRequirements': sorted(set(requirements) - enabled),
                             'versionMin': metadata.get('versionMin', '').strip(),
                             'versionMax': metadata.get('versionMax', '').strip(),
                             'cells': sorted(data['headers'])})
    overlaps = []
    for left, right in itertools.combinations(maps, 2):
        cells = sorted(set(left['cells']) & set(right['cells']))
        if cells:
            overlaps.append({'left': left['folder'], 'right': right['folder'], 'cells': cells})
    lua_collisions = {path: entries for path, entries in lua_files.items() if len({entry['sha256'] for entry in entries}) > 1}
    tile_collisions = {number: entries for number, entries in tile_files.items() if len({(entry['pack'], entry['sha256']) for entry in entries}) > 1}
    return {'maps': maps, 'overlaps': overlaps, 'luaCollisions': lua_collisions, 'tileCollisions': tile_collisions,
            'enabledNotFoundInWorkshop': sorted(enabled - found),
            'limits': 'Static headers, dependency metadata and file hashes; does not prove room, object, NPC, FPS or runtime compatibility.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--workshop', type=Path, default=Path('/home/j0schi/SteamLibrary/steamapps/workshop/content/108600'))
    parser.add_argument('--config', type=Path, default=Path('/home/j0schi/Zomboid/mods/default.txt'))
    args = parser.parse_args()
    report = inspect(args.workshop, args.config)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if report['overlaps'] or report['luaCollisions'] or report['tileCollisions']:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
