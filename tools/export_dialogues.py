"""Export the editable SQLite dialogue database into the Java runtime resource."""
import argparse
from pathlib import Path
import sqlite3

ROOT = Path(__file__).resolve().parents[1]
DATABASE = ROOT / "db/dialogues.sqlite3"
OUTPUT = ROOT / "src/main/resources/personal-knox/dialogues.tsv"


def render(database=DATABASE):
    with sqlite3.connect(f"{database.as_uri()}?mode=ro", uri=True) as connection:
        rows = connection.execute(
            "SELECT id, topic, condition, text_ru FROM dialogue ORDER BY id"
        ).fetchall()
    if not rows:
        raise ValueError("Dialogue database is empty")
    lines = []
    for identifier, topic, condition, text in rows:
        if condition not in {"default", "low_morale", "low_trust", "brave"}:
            raise ValueError(f"Unknown condition: {condition}")
        if not text.strip() or any(ord(char) < 32 for char in text):
            raise ValueError(f"Invalid dialogue text: {identifier}")
        if any("\t" in field or "\n" in field for field in (topic, condition, text)):
            raise ValueError("Tabs and newlines are not supported in dialogue fields")
        lines.append(f"{identifier}\t{topic}\t{condition}\t{text}")
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Check that the Java resource matches SQLite")
    args = parser.parse_args()
    content = render()
    if args.check:
        if not OUTPUT.exists() or OUTPUT.read_text() != content:
            raise SystemExit("Java resource is out of date; run tools/export_dialogues.py")
        print("SQLite and Java resource match")
    else:
        OUTPUT.parent.mkdir(parents=True, exist_ok=True)
        OUTPUT.write_text(content)
        print(OUTPUT)


if __name__ == "__main__":
    main()
