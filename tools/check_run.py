"""Summarize the latest PersonalKnox run from console.txt without launching the game."""
import argparse
import re
from pathlib import Path


def summarize(text: str) -> dict:
    core = re.findall(r'\[PersonalKnox\] Java core ([\d.]+) loaded', text)
    return {
        'core': core[-1] if core else '',
        'lua': bool(re.search(r'\[PersonalKnox\] Lua addon loaded', text)),
        'capabilities': len(re.findall(r'\[PersonalKnox\]\[capabilities\]', text)),
        'care': len(re.findall(r'\[PersonalKnox\]\[care\]', text)),
        'upkeep': len(re.findall(r'\[PersonalKnox\]\[upkeep\]', text)),
        'probe': len(re.findall(r'\[PersonalKnox\]\[probe\]', text)),
        'errors': [line.strip() for line in text.splitlines()
                   if 'PersonalKnox' in line and re.search(r'error|exception|failed', line, re.I)],
        'exited': 'GameThread exited' in text,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--console', type=Path, default=Path.home() / 'Zomboid/console.txt')
    parser.add_argument('--expect', default='0.4.36')
    args = parser.parse_args()
    report = summarize(args.console.read_text(errors='replace'))
    ok = report['core'] == args.expect and report['lua'] and not report['errors']
    print(f"core={report['core'] or 'missing'} expected={args.expect} {'PASS' if report['core'] == args.expect else 'FAIL'}")
    print(f"lua={report['lua']} capabilities={report['capabilities']} "
          f"care={report['care']} upkeep={report['upkeep']} probe={report['probe']}")
    print(f"errors={len(report['errors'])} exited={report['exited']}")
    for error in report['errors'][:5]:
        print('  ' + error[:200])
    return 0 if ok else 1


if __name__ == '__main__':
    raise SystemExit(main())
