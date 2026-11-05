"""Exercise supply proposals against usable, unsafe, empty and unsupported inventory items."""
from pathlib import Path
from lupa.lua51 import LuaRuntime

root = Path(__file__).resolve().parents[1]
lua = LuaRuntime(unpack_returned_tuples=True)
lua.globals().package.path = str(root / 'common/media/lua/shared/?.lua') + ';' + str(root / 'common/media/lua/client/?.lua')
lua.execute('''
function getText(key) return key end
function isClient() return false end
function getGameTime() return {getWorldAgeHours=function() return 1 end} end
function instanceof(item,kind) return item.kind==kind end
Events={EveryTenMinutes={Add=function() end},OnGameStart={Add=function() end}}
Fluid={Water={}}
local seen,poisonChecks={},{n=0}
local java={
    isSupplyFood=function(_,hunger,canEat,rotten,burnt,dangerRaw,cooked,knownPoison)
        return hunger<0 and canEat and not rotten and not burnt and (not dangerRaw or cooked) and not knownPoison
    end,
    isSupplyWater=function(_,amount,pure,tainted) return amount>0 and pure and not tainted end,
    isCleanBandage=function(_,name,canBandage,infected) return canBandage and not infected and name~='Base.RippedSheetsDirty' end,
    supplyPlan=function(_,food,water,medicine,weapon)
        seen={food=food,water=water,medicine=medicine,weapon=weapon}
        if food<2 or water<1 or medicine<2 then return 'essentials' end
        return weapon and 'none' or 'weapons'
    end,
}
PersonalKnoxAPI={getInstance=function() return java end}
local contents={}
local function inventory(items)
    return {getItems=function() return {size=function() return #items end,get=function(_,i) return items[i+1] end} end,
        getCapacityWeight=function() return 1 end}
end
local building={}
local square={getBuilding=function() return building end}
local body={kind='IsoNpcPlayer',isDead=function() return false end,
    getX=function() return 0 end,getY=function() return 0 end,getZ=function() return 0 end,
    getInventory=function() return inventory(contents) end,getMaxWeight=function() return 50 end,
    hasNoDangerousTargets=function() return true end,hasEnemyNpcTargets=function() return false end,
    isInActiveCombat=function() return false end,
    isKnownPoison=function(_,item) poisonChecks.n=poisonChecks.n+1;return item.knownPoison==true end}
local data={}
local player={getX=body.getX,getY=body.getY,getZ=body.getZ,getCurrentSquare=function() return square end,
    getModData=function() return data end,Say=function() end}
local npc={getWorldObject=function() return body end}
function getCell() return {getGridSquare=function() return nil end} end
local function item(options)
    local data=options or {}
    data.getFullType=function() return data.name or 'Base.Test' end
    data.isWaterSource=function() return data.source==true end
    data.isCanBandage=function() return data.bandage==true end
    data.isInfected=function() return data.infected==true end
    data.isBroken=function() return data.broken==true end
    data.getHungerChange=function() if data.unsupported then error('Unavailable food API') end;return data.hunger or -.08 end
    data.getScriptItem=function() return {isCantEat=function() return data.cannotEat==true end} end
    data.isRotten=function() return data.rotten==true end
    data.isBurnt=function() return data.burnt==true end
    data.isbDangerousUncooked=function() return data.rawDanger==true end
    data.isCooked=function() return data.cooked==true end
    data.getPoisonPower=function() error('Must use NPC knowledge, not hidden poison data') end
    data.getFluidContainer=function()
        if data.noFluid then return nil end
        return {getAmount=function() return data.amount or 0 end,
            isPureFluid=function(_,fluid) assert(fluid==Fluid.Water);return data.pure==true end,
            isTainted=function() return data.tainted==true end}
    end
    data.getInventory=function() return inventory(data.contents or {}) end
    return data
end
local supply=require 'PersonalKnox/Supplies'
local announced=0
supply.install({isMember=function() return true end,say=function() announced=announced+1 end,journal=function() end})
contents={item{kind='Food'},item{kind='Food',rawDanger=true,cooked=true},
    item{source=true,amount=.5,pure=true},item{bandage=true},item{bandage=true},item{kind='HandWeapon'},
    {getFullType=function() error('Do not scan beyond the fulfilled reserve') end}}
supply.propose(player,npc)
assert(seen.food==2 and seen.water==1 and seen.medicine==2 and seen.weapon)
assert(announced==0,'A stocked NPC should not announce a new plan')
contents={item{kind='Food',rotten=true},item{kind='Food',burnt=true},item{kind='Food',rawDanger=true},
    item{kind='Food',cannotEat=true},item{kind='Food',hunger=0},item{kind='Food',knownPoison=true},
    item{source=true,amount=0,pure=true},item{source=true,amount=1,pure=true,tainted=true},
    item{source=true,amount=1,pure=false},item{source=true,noFluid=true},
    item{bandage=true,name='Base.RippedSheetsDirty'},item{bandage=true,infected=true},item{kind='HandWeapon',broken=true}}
supply.propose(player,npc)
assert(seen.food==0 and seen.water==0 and seen.medicine==0 and not seen.weapon,
    'Unsafe/inedible food, empty or tainted water, dirty dressings and broken weapons do not satisfy reserves')
assert(announced==1 and supply.pending[npc].kind=='essentials','Unsuitable stocks must produce an actual confirmed supply proposal')
contents={item{kind='Food',unsupported=true},item{kind='InventoryContainer',contents={item{kind='Food'},item{source=true,amount=.2,pure=true}}}}
supply.propose(player,npc)
assert(seen.food==1 and seen.water==1,'One unsupported item must not stop inspection of the remaining bags')
assert(poisonChecks.n>0,'Ask the NPC about known poison')
''')
print('Supply adapter readiness checks passed: actual plan counts, safe food/water, clean dressings, broken weapons, nested bags and unsupported-item fallback.')
