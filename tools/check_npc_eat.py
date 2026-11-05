"""Exercise real NPC eating selection, bounded scans and action dispatch with game doubles."""
import argparse
from pathlib import Path
from lupa.lua51 import LuaRuntime

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--source', type=Path, default=root / 'common/media/lua/client/PersonalKnox/NpcEat.lua')
args = parser.parse_args()
lua = LuaRuntime(unpack_returned_tuples=True)
lua.globals().package.path = str(root/'common/media/lua/shared/?.lua')+';'+str(root/'common/media/lua/client/?.lua')
lua.globals().npcEatSource = args.source.read_text()
lua.execute('''
local tick,reset,now,roster= nil,nil,0,{}
local queued,errors,reads,bodyReads,perfReads={}, {},0,0,0
local constrained,eatEnabled,drinkEnabled=false,true,true
Events={OnGameStart={Add=function(fn) reset=fn end},OnPlayerUpdate={Add=function(fn) tick=fn end}}
function isClient() return false end
function getTimestampMs() return now end
function print(message) errors[#errors+1]=tostring(message) end
function instanceof(item,kind) return item.kind==kind end
Fluid={Water={}}
CharacterStat={HUNGER={},THIRST={}}
local player={isDead=function() return false end}
function getSpecificPlayer() return player end
package.preload['PersonalKnox/Settings']=function() return {
    autoEat=function() return eatEnabled end,autoDrink=function() return drinkEnabled end,
    diagnosticsEnabled=function() return false end,
} end
package.preload['PersonalKnox/Perf']=function() return {skip=function() perfReads=perfReads+1;return constrained end} end
PersonalKnoxAPI={getInstance=function() return {
    isSupplyFood=function(_,hunger,canEat,rotten,burnt,raw,cooked,poison)
        return hunger<0 and canEat and not rotten and not burnt and (not raw or cooked) and not poison
    end,
    isSupplyWater=function(_,amount,pure,tainted) return amount>0 and pure and not tainted end,
} end}
local function item(options)
    local t=options or {}
    t.IsFood=function() return t.kind=='Food' end -- Also supported by the previous adapter.
    t.getHungerChange=function() if t.unsupported then error('Unavailable food metadata') end;return -.1 end
    t.getScriptItem=function() return {isCantEat=function() return t.cannotEat==true end} end
    t.isRotten=function() return t.rotten==true end
    t.isBurnt=function() return t.burnt==true end
    t.isbDangerousUncooked=function() return t.raw==true end
    t.isCooked=function() return t.cooked==true end
    t.getPoisonPower=function() error('Hidden poison must not be inspected') end
    t.isWaterSource=function() return t.source==true end
    t.getFluidContainer=function()
        if not t.source then return nil end
        return {getAmount=function() return t.amount or 0 end,isPureFluid=function(_,f) assert(f==Fluid.Water);return t.pure==true end,
            isTainted=function() return t.tainted==true end,
            getPrimaryFluid=function() return {getFluidTypeString=function() return 'Water' end} end}
    end
    return t
end
local nextId=0
local function npc(contents,hunger,thirst,count,lateIndex)
    nextId=nextId+1;local id=nextId
    local b={hunger=hunger,thirst=thirst,contents=contents}
    b.isDead=function() return b.dead==true end
    b.hasTimedActions=function() return b.busy==true end
    b.hasNoDangerousTargets=function() return not b.danger end
    b.hasEnemyNpcTargets=function() return b.enemy==true end
    b.isInActiveCombat=function() return b.combat==true end
    b.isKnownPoison=function(_,food) return food.poison==true end
    b.getStats=function() return {get=function(_,stat) return stat==CharacterStat.HUNGER and b.hunger or b.thirst end} end
    b.getInventory=function() return {getItems=function() return {
        size=function() return count or #b.contents end,
        get=function(_,i) reads=reads+1;return count and (i==lateIndex and contents[1] or item{}) or b.contents[i+1] end,
    } end} end
    b.Eat=function() error('Do not bypass the eating action') end
    return {body=b,getId=function() return id end,getWorldObject=function() bodyReads=bodyReads+1;return b end}
end
local function action(kind)
    return {new=function(_,body,item,pct) assert(pct==1);return {kind=kind,body=body,item=item} end}
end
ISEatFoodAction=action('food');ISDrinkFluidAction=action('water')
ISTimedActionQueue={add=function(a) queued[#queued+1]=a end}
local module=assert(loadstring(npcEatSource))()
module.install({roster=function() return roster end})
local function fresh()
    queued={};errors={};reads=0;bodyReads=0;perfReads=0;now=0
    constrained=false;eatEnabled=true;drinkEnabled=true;reset()
end
local function step() now=now+10000;tick();assert(#errors==0,tostring(errors[1])) end

fresh()
local safeFood=item{kind='Food'}
roster={npc({item{kind='Food',rotten=true},item{kind='Food',burnt=true},item{kind='Food',poison=true},
    item{kind='Food',raw=true},item{kind='Food',unsupported=true},safeFood},.8,0)}
step();assert(#queued==1 and queued[1].item==safeFood,'Must skip unsafe food and continue past unsupported metadata')
local lastPerf=perfReads;tick();assert(perfReads==lastPerf,'No FPS checks between scheduled polls')
fresh()
local water=item{source=true,amount=1,pure=true}
roster={npc({item{source=true,amount=1,pure=true,tainted=true},item{source=true,amount=1,pure=false},
    item{source=true,amount=0,pure=true},water},0,.8)}
step();assert(#queued==1 and queued[1].kind=='water' and queued[1].item==water,'Only clean, nonempty pure water')
fresh();roster={npc({water},.8,.8)}
step();assert(#queued==1 and queued[1].kind=='water','Missing food must not prevent available drinking')
for _,flag in ipairs({'danger','enemy','combat','busy','dead'}) do
    fresh();local n=npc({safeFood},.8,0);n.body[flag]=true;roster={n}
    step();assert(#queued==0 and reads==0,'Do not interrupt danger/combat or existing actions: '..flag)
end
fresh();local n=npc({safeFood},.8,0);n.body.hasNoDangerousTargets=nil;roster={n}
step();assert(#queued==0 and reads==0,'Unknown danger API must not permit a new action')
fresh();eatEnabled=false;drinkEnabled=false;roster={npc({safeFood,water},.8,.8)}
step();assert(#queued==0 and reads==0,'Disabled automation must not inspect inventory items')
fresh();roster={npc({safeFood},.8,0,100000,200)}
step();assert(reads==128 and #queued==0,'Bound a huge inventory per poll')
step();assert(reads==201 and #queued==1 and queued[1].item==safeFood,'Resume beyond the first budget')
fresh();roster={}
for i=1,4 do roster[i]=npc({safeFood},.8,0) end
step();assert(#queued==2 and bodyReads==2)
step();assert(#queued==4 and bodyReads==4 and queued[1].body~=queued[3].body,'Rotate NPCs instead of starving later members')
fresh();constrained=true
roster={npc({safeFood},.8,0,100000,40),npc({safeFood},.8,0,100000,40)}
step();assert(reads==32 and bodyReads==1 and #queued==0)
step();assert(reads==64 and bodyReads==2 and #queued==0)
step();assert(reads==73 and bodyReads==3 and #queued==1,'Low FPS reduces budgets while essential needs continue')
queued={};reads=0;now=0;reset();step()
assert(reads==32 and #queued==0,'World reset clears NPC/item cursors')
''')
print('NPC eating checks passed: shared safe-food/water policy, threat/busy guards, ordinary queued actions, 128/32 item budgets, NPC rotation, resumed search and reset. Animations/consumption still require the game.')
