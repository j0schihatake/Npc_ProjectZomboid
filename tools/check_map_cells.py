"""Read enabled local map files; report cell overlaps, not runtime compatibility."""
import argparse
import itertools
import json
from pathlib import Path
import re


def inspect(workshop, config):
    enabled = set(re.findall(r'^\s*mod\s*=\s*(.+?)\s*,?\s*$', config.read_text(), re.M))
    maps = []
    found = set()
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
                    if lots:
                        entry['lots'] = lots[1]
        for folder, data in sorted(folders.items()):
            if data['headers']:
                maps.append({'mods': sorted(active), 'folder': folder, 'lots': data['lots'],
                             'versionFolder': selected.name, 'cells': sorted(data['headers'])})
    overlaps = []
    for left, right in itertools.combinations(maps, 2):
        cells = sorted(set(left['cells']) & set(right['cells']))
        if cells:
            overlaps.append({'left': left['folder'], 'right': right['folder'], 'cells': cells})
    return {'maps': maps, 'overlaps': overlaps, 'enabledNotFoundInWorkshop': sorted(enabled - found),
            'limits': 'Cell headers only; does not prove room, object, NPC, FPS or runtime compatibility.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--workshop', type=Path, default=Path('/home/j0schi/SteamLibrary/steamapps/workshop/content/108600'))
    parser.add_argument('--config', type=Path, default=Path('/home/j0schi/Zomboid/mods/default.txt'))
    args = parser.parse_args()
    report = inspect(args.workshop, args.config)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if report['overlaps']:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
