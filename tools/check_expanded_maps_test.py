"""Check additive profile assembly without touching the user's mod configuration."""
from apply_expanded_maps import expanded_text

source = 'VERSION = 1,\nmods\n{\n    mod = ZombieBuddy,\n    mod = MyNewSetting,\n}\nmaps\n{\n}\n'
result = expanded_text(source)
assert '    mod = MyNewSetting,\n' in result
assert result.count('mod = CamdenCountyB42,') == 1
assert expanded_text(result) == result, 'Applying twice must not duplicate mods'
assert result.endswith('maps\n{\n}\n'), 'Preserve the map-order section'
try:
    expanded_text(source.replace('    mod = ZombieBuddy,\n', ''))
    raise AssertionError('Unknown configuration layout accepted')
except ValueError:
    pass
print('Expanded-map profile checks passed: existing choices, no duplicates, preserved map order and unknown-format rejection.')
