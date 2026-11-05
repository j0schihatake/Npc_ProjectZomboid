"""Run the real Chatter adapter in the installed game's Kahlua runtime, without a world."""
import argparse
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
GAME = Path('/home/j0schi/SteamLibrary/steamapps/common/ProjectZomboid/projectzomboid')
FIXTURE = '''
assert(math.random==nil,'This check must use the game Kahlua math library')
local update,reset
local now,rate,enabled,skip,diagnostics=30000,3,true,false,false
local said,scanned,rolled,low,high=0,0,0,nil,nil
local errors={}
function print(message) errors[#errors+1]=tostring(message) end
function getTimestampMs() return now end
function isClient() return false end
function ZombRand(minimum,maximum)
    rolled=rolled+1;low,high=minimum,maximum
    return minimum
end
local player={isDead=function() return false end,isTargetedByZombie=function() return false end,
    isAiming=function() return false end,getX=function() return 0 end,getY=function() return 0 end}
function getSpecificPlayer() return player end
local body={isDead=player.isDead,getX=function() return 1 end,getY=function() return 0 end}
local npc={getWorldObject=function() return body end}
local modules={
    ['PersonalKnox/Settings']={speechEnabled=function() return enabled end,
        chatterFrequency=function() return rate end,diagnosticsEnabled=function() return diagnostics end},
    ['PersonalKnox/Perf']={skip=function() return skip end},
    ['PersonalKnox/Util']={id=function() return 'test-npc' end},
}
function require(name) assert(modules[name],name);return modules[name] end
Events={OnGameStart={Add=function(fn) reset=fn end},OnPlayerUpdate={Add=function(fn) update=fn end}}
local chatter=assert(loadstring(chatterSource))()
chatter.install({roster=function() scanned=scanned+1;return {npc} end,
    say=function(p,n,topic) assert(p==player and n==npc and topic=='smalltalk');said=said+1 end})
reset();update()
assert(#errors==0,'Chatter raised an error after speaking: '..tostring(errors[1]))
assert(said==1 and scanned==1 and rolled==1 and low==90000 and high==180001,'Normal interval')
now=60000;update();now=90000;update()
assert(said==1 and scanned==1 and rolled==1,'Cooldown must skip roster work and random calls')
now=120000;update()
assert(said==2 and scanned==2 and rolled==2,'Speak again only when the interval has elapsed')
enabled=false;now=300000;update()
assert(said==2 and scanned==2 and rolled==2,'Speech off must be inert')
enabled=true;rate=1;update()
assert(said==2 and scanned==2,'Chatter off must not scan')
rate=2;reset();update()
assert(low==240000 and high==420001 and said==3,'Rare interval')
rate=4;reset();update()
assert(low==45000 and high==90001 and said==4,'Frequent interval')
skip=true;now=600000;update()
assert(said==4 and scanned==4,'Performance guard must skip chatter')
skip=false;diagnostics=true;reset();update()
assert(said==5 and #errors==1 and string.find(errors[1],'[chatter]',1,true),'Diagnostic output')
'''

RUNNER = '''import java.nio.file.*;
import java.util.Arrays;
public final class CheckChatter {
    public static void main(String[] args) throws Exception {
        Class<?> platformType=Class.forName("se.krka.kahlua.j2se.J2SEPlatform");
        Object platform=platformType.getMethod("getInstance").invoke(null);
        Object env=platformType.getMethod("newEnvironment").invoke(platform);
        Class<?> table=Class.forName("se.krka.kahlua.vm.KahluaTable");
        table.getMethod("rawset",Object.class,Object.class).invoke(env,"chatterSource",Files.readString(Path.of(args[0])));
        Class<?> compiler=Class.forName("se.krka.kahlua.luaj.compiler.LuaCompiler");
        Object closure=compiler.getMethod("loadstring",String.class,String.class,table)
            .invoke(null,Files.readString(Path.of(args[1])),"PersonalKnoxChatterCheck",env);
        Class<?> threadType=Class.forName("se.krka.kahlua.vm.KahluaThread");
        Object thread=threadType.getConstructor(Class.forName("se.krka.kahlua.vm.Platform"),table)
            .newInstance(platform,env);
        threadType.getField("debugOwnerThread").set(thread,Thread.currentThread());
        Object[] result=(Object[])threadType.getMethod("pcall",Object.class,Object[].class)
            .invoke(thread,closure,new Object[0]);
        if (!Boolean.TRUE.equals(result[0])) throw new AssertionError(Arrays.toString(result));
        System.out.println("Game Kahlua chatter checks passed: no math.random, speech, cooldown, rate options, disabled/performance guards and diagnostics.");
    }
}
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--game', type=Path, default=GAME)
    parser.add_argument('--source', type=Path, default=ROOT / 'common/media/lua/client/PersonalKnox/Chatter.lua')
    args = parser.parse_args()
    with tempfile.TemporaryDirectory(prefix='personalknox-chatter-') as tmp:
        temp = Path(tmp)
        (temp / 'CheckChatter.java').write_text(RUNNER)
        (temp / 'fixture.lua').write_text(FIXTURE)
        subprocess.run(['javac', '--release', '17', '-d', tmp, str(temp / 'CheckChatter.java')], check=True)
        subprocess.run([str(args.game / 'jre64/bin/java'), '-cp', tmp + ':' + str(args.game / 'projectzomboid.jar'),
                        'CheckChatter', str(args.source.resolve()), str(temp / 'fixture.lua')], cwd=args.game, check=True)


if __name__ == '__main__':
    main()
