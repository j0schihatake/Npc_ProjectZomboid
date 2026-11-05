"""Verify map-audit version selection, effective map metadata and overlap detection."""
from pathlib import Path
from tempfile import TemporaryDirectory
from check_map_cells import inspect


with TemporaryDirectory() as directory:
    root = Path(directory)
    config = root / 'mods.txt'
    config.write_text('mods\n{\n mod = First,\n mod = Second,\n}\n')
    first = root / '100/mods/First'
    second = root / '200/mods/Second'
    for path in [first / 'common', first / '42.20.4', first / '42.22', second / 'common', second / '42']:
        path.mkdir(parents=True)
    (first / '42.20.4/mod.info').write_text('id=First\nrequire=\\Tiles\nversionMin=42.20.4\n')
    (first / '42.22/mod.info').write_text('id=First\nrequire=IncorrectFutureDependency\n')
    (second / '42/mod.info').write_text('id=Second\n')
    a = first / 'common/media/maps/FirstMap'
    overlay = first / '42.20.4/media/maps/FirstMap'
    future = first / '42.22/media/maps/FirstMap'
    b = second / 'common/media/maps/SecondMap'
    for folder in [a, overlay, future, b]:
        folder.mkdir(parents=True)
    (a / 'map.info').write_text('lots=Muldraugh, KY\n')
    (a / '-1_2.lotheader').touch()
    (overlay / '-1_2.lotheader').touch()  # Same logical file across layers, counted once.
    (overlay / 'map.info').write_text('title=Standalone\n')
    (future / '80_80.lotheader').touch()
    (b / 'map.info').write_text('lots=Muldraugh, KY\n')
    (b / '-1_2.lotheader').touch()
    report = inspect(root, config)
    assert len(report['maps']) == 2
    first_map = report['maps'][0]
    assert first_map['versionFolder'] == '42.20.4'
    assert first_map['cells'] == [(-1, 2)]
    assert first_map['lots'] == '', 'A versioned standalone map must not inherit stale lots metadata'
    assert first_map['missingEnabledRequirements'] == ['Tiles']
    assert first_map['workshop'] == '100'
    assert report['overlaps'] == [{'left': 'FirstMap', 'right': 'SecondMap', 'cells': [(-1, 2)]}]
    config.write_text(config.read_text().replace(' mod = Second,', ' mod = Tiles,'))
    report = inspect(root, config)
    assert len(report['maps']) == 1 and not report['overlaps']
    assert report['maps'][0]['missingEnabledRequirements'] == []
print('Map audit checks passed: current version, common overlay, negative cells, dependencies, real overlap and disabled maps.')
