"""Summarize the latest PersonalKnox run from console.txt without launching the game."""
import argparse
import re
from pathlib import Path


def summarize(text: str) -> dict:
    core = list(re.finditer(r'\[PersonalKnox\] Java core ([\d.]+) loaded', text))
    lua = list(re.finditer(r'\[PersonalKnox\] Lua addon(?: (\S+))? loaded', text))
    return {
        'core': core[-1].group(1) if core else '',
        'lua': bool(lua),
        'lua_version': (lua[-1].group(1) or '') if lua else '',
        'lua_after_core': bool(lua and core and lua[-1].start() > core[-1].start()),
        'capabilities': len(re.findall(r'\[PersonalKnox\]\[capabilities\]', text)),
        'care': len(re.findall(r'\[PersonalKnox\]\[care\]', text)),
        'upkeep': len(re.findall(r'\[PersonalKnox\]\[upkeep\]', text)),
        'probe': len(re.findall(r'\[PersonalKnox\]\[probe\]', text)),
        'errors': [line.strip() for line in text.splitlines()
                   if 'PersonalKnox' in line and re.search(r'error|exception|failed', line, re.I)],
        'exited': 'GameThread exited' in text,
    }


def startup_matches(report: dict, expected_core: str, expected_lua: str) -> bool:
    # A legacy unversioned Lua banner proves loading, not a particular release.
    return (report['core'] == expected_core and report['lua'] and report['lua_after_core']
            and report['lua_version'] == expected_lua and not report['errors'])


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--console', type=Path, default=Path.home() / 'Zomboid/console.txt')
    parser.add_argument('--expect', default='0.5.5')
    parser.add_argument('--expect-lua', help='Expected Lua release; defaults to --expect. May differ after a Lua-only update.')
    args = parser.parse_args()
    report = summarize(args.console.read_text(errors='replace'))
    expected_lua = args.expect_lua or args.expect
    ok = startup_matches(report, args.expect, expected_lua)
    print(f"core={report['core'] or 'missing'} expected={args.expect} {'PASS' if report['core'] == args.expect else 'FAIL'}")
    print(f"lua={report['lua']} version={report['lua_version'] or 'unverified'} expected={expected_lua} "
          f"{'PASS' if report['lua_version'] == expected_lua else 'FAIL'} capabilities={report['capabilities']} "
          f"care={report['care']} upkeep={report['upkeep']} probe={report['probe']}")
    print(f"errors={len(report['errors'])} exited={report['exited']}")
    for error in report['errors'][:5]:
        print('  ' + error[:200])
    return 0 if ok else 1


if __name__ == '__main__':
    raise SystemExit(main())
