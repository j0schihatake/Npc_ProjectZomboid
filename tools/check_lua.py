"""Check the Lua 5.1 adapters and the confirmation/dispatch contract using game doubles."""
from pathlib import Path
from lupa.lua51 import LuaRuntime

root = Path(__file__).resolve().parents[1]
lua = LuaRuntime(unpack_returned_tuples=True)
for file in (root/'common/media/lua').rglob('*.lua'):
    lua.execute('assert(loadstring(...))', file.read_text())
lua.globals().package.path = str(root/'common/media/lua/shared/?.lua')+';'+str(root/'common/media/lua/client/?.lua')
lua.execute('''
function getText(key, ...) return key end
Events={EveryTenMinutes={Add=function(fn) tick=fn end}, OnFillWorldObjectContextMenu={Add=function(fn) contextCallback=fn end},
    EveryOneMinute={Add=function(fn) reactionTick=fn end}, OnGameStart={Add=function(fn) reactionReset=fn end},
    OnKeyPressed={Add=function(fn) recallKey=fn end}}
package.preload.KnoxEventLoader=function() return {} end
PersonalKnoxAPI={getInstance=function() return {supplyPlan=function() return "essentials" end} end}
local Supply=require "PersonalKnox/Supplies"
local building={}
local items={size=function() return 0 end}
local weight=1
local inv={getItems=function() return items end, getCapacityWeight=function() return weight end, getCapacity=function() return 50 end}
local body={getX=function() return 0 end,getY=function() return 0 end,getZ=function() return 0 end,
    isDead=function() return false end,getInventory=function() return inv end,getMaxWeight=function() return 50 end,
    kind='IsoNpcPlayer',hasNoDangerousTargets=function() return true end,
    hasEnemyNpcTargets=function() return false end,isInActiveCombat=function() return false end}
local data={}
local square={getBuilding=function() return building end}
local player={getModData=function() return data end,getCurrentSquare=function() return square end,
    getX=body.getX,getY=body.getY,getZ=body.getZ,Say=function() end}
local npc={getWorldObject=function() return body end}
local zombieList={size=function() return 0 end}
function getCell() return {getZombieList=function() error('No global zombie scan') end,getGridSquare=function() return nil end} end
function instanceof(object,kind) return object.kind==kind end
function getGameTime() return {getWorldAgeHours=function() return 10 end} end
function isClient() return false end
function getSpecificPlayer() return player end
local member=true
Supply.install({isMember=function() return member end,journal=function() end,say=function() announced=true end})
selectedGroupNpcs={"original"};lootAmmo=true;lootFood=false
local selectedBefore=selectedGroupNpcs
local dispatched=0
function tellNpcLoot(_, b)
    assert(announced,"Intent must be announced first")
    assert(selectedGroupNpcs[1]==npc and b==building)
    assert(lootFood and lootWater and lootFirstAid and not lootGuns)
    dispatched=dispatched+1
end
local function menu()
    local result={options={}}
    function result:addOption(name,target,fn,arg)
        self.options[#self.options+1]={name=name,fn=fn,target=target,arg=arg}
    end
    Supply.addOptions(result,player,npc)
    return result
end
Supply.propose(player,npc)
assert(dispatched==0,"Default confirmation must block dispatch")
local first=menu()
assert(#first.options==6)
first.options[4].fn(player,npc)
assert(dispatched==1,"Confirm must dispatch")
assert(selectedGroupNpcs==selectedBefore and lootAmmo==true and lootFood==false,"Knox UI state must be restored")
Supply.propose(player,npc)
member=false
local second=menu()
second.options[4].fn(player,npc)
assert(dispatched==1,"Membership must be rechecked at approval")
member=true
data.PersonalKnox.confirmPlans=false
Supply.propose(player,npc)
assert(dispatched==2,"Automatic mode dispatches after announcing")
function tellNpcLoot() error("simulated Knox failure") end
Supply.propose(player,npc)
assert(selectedGroupNpcs==selectedBefore and lootAmmo==true and lootFood==false,"Failure must still restore UI state")
Supply.enabled[npc]=true
data.PersonalKnox.confirmPlans=true
Supply.visited[npc]=nil
tick()
assert(Supply.pending[npc]~=nil,"Enabled autonomy proposes a plan")
local decline=menu()
decline.options[5].fn(player,npc)
assert(Supply.pending[npc]==nil,"Decline must cancel")
tick()
assert(Supply.pending[npc]==nil,"Declined building must not be proposed repeatedly")
local beforeHouse=dispatched
function tellNpcLoot(_,b)
    assert(b==building and selectedGroupNpcs[1]==npc)
    assert(lootGuns and lootWeapons and lootAmmo and lootFood and lootWater and lootFirstAid)
    dispatched=dispatched+1
end
Supply.inspectHouse(player,npc)
assert(Supply.pending[npc].kind=='house' and dispatched==beforeHouse,"Explicit house inspection uses confirmation")
menu().options[4].fn(player,npc)
assert(dispatched==beforeHouse+1,"House inspection includes supplies and weapon categories")
assert(selectedGroupNpcs==selectedBefore and lootAmmo==true and lootFood==false,"House inspection restores Knox UI state")
Supply.inspectHouse(player,npc)
weight=45
menu().options[4].fn(player,npc)
assert(dispatched==beforeHouse+1 and Supply.pending[npc]==nil,"Recheck capacity at approval")
weight=1
require "PersonalKnox/Client"
assert(contextCallback,"Client must register its menu")
''')
print('Lua 5.1 checks passed: syntax, confirmation, dispatch, membership recheck, error cleanup, rejection cooldown, menu registration.')
