"""Check public Knox transfer metadata without starting the game or creating actions."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
GAME = Path('/home/j0schi/SteamLibrary/steamapps/common/ProjectZomboid/projectzomboid')
WORKSHOP = GAME.parents[2] / 'workshop/content/108600'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--game', type=Path, default=GAME)
    parser.add_argument('--knox', type=Path)
    args = parser.parse_args()
    knox = args.knox
    if knox is None:
        candidates = sorted(WORKSHOP.glob('*/mods/*/42/media/java/KnoxEventExpanded.jar'))
        if len(candidates) != 1:
            parser.error('Pass --knox pointing to the installed KnoxEventExpanded.jar')
        knox = candidates[0]
    config = json.loads((args.game / 'ProjectZomboid64.json').read_text())
    with tempfile.TemporaryDirectory(prefix='personalknox-transfer-api-') as tmp:
        temp = Path(tmp)
        source = temp / 'CheckTransfer.java'
        source.write_text('''package personal.knox;
public final class CheckTransfer {
    public static void main(String[] args) throws Exception {
        KnoxTransferProbeTest.run();
        var loader = ClassLoader.getSystemClassLoader();
        var actor = Class.forName("knox.event.npc.MetaNpcCharacter", false, loader);
        String status = KnoxTransferProbe.inspect(actor, actor,
            Class.forName("knox.event.npc.actions.ISNpcInventoryTransferAction", false, loader),
            Class.forName("zombie.inventory.InventoryItem", false, loader),
            Class.forName("zombie.inventory.ItemContainer", false, loader),
            Class.forName("knox.event.npc.actions.MetaBaseAction", false, loader));
        if (!status.equals("public_api_present")) throw new AssertionError(status);
        System.out.println("Knox transfer public metadata: " + status + "; no actions created or queued");
    }
}
''')
        subprocess.run(['javac', '--release', '17', '-d', str(temp),
                        str(ROOT / 'src/main/java/personal/knox/KnoxTransferProbe.java'),
                        str(ROOT / 'src/test/java/personal/knox/KnoxTransferProbeTest.java'),
                        str(source)], check=True)
        classpath = [str(temp), str(knox.resolve())]
        classpath.extend(str((args.game / entry).resolve()) for entry in config['classpath'])
        subprocess.run([str(args.game / 'jre64/bin/java'), '-ea', '-cp',
                        os.pathsep.join(classpath), 'personal.knox.CheckTransfer'], check=True)


if __name__ == '__main__':
    main()
