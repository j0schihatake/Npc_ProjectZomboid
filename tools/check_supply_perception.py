"""Supply runs must respect native NPC threat knowledge and bound fallback perception."""
from pathlib import Path
from lupa.lua51 import LuaRuntime

root=Path(__file__).resolve().parents[1]
lua=LuaRuntime(unpack_returned_tuples=True)
lua.globals().package.path=str(root/'common/media/lua/shared/?.lua')+';'+str(root/'common/media/lua/client/?.lua')
lua.execute('''
function getText(key) return key end
function isClient() return false end
function instanceof(object,kind) return object.kind==kind end
Events={EveryTenMinutes={Add=function() end}}
local nativeSafe,nativeEnemy,nativeCombat,visible=true,false,false,true
local squares,visibility=0,0
local scene={}
local building={}
local data={}
local inv={getItems=function() return {size=function() return 0 end} end,getCapacityWeight=function() return 1 end}
local function see() visibility=visibility+1;return visible end
local body={kind='IsoNpcPlayer',getX=function() return 0 end,getY=function() return 0 end,getZ=function() return 0 end,
 isDead=function() return false end,getInventory=function() return inv end,getMaxWeight=function() return 50 end,
 hasNoDangerousTargets=function() return nativeSafe end,hasEnemyNpcTargets=function() return nativeEnemy end,
 isInActiveCombat=function() return nativeCombat end,CanSee=see}
local player={getX=body.getX,getY=body.getY,getZ=body.getZ,CanSee=see,
 getModData=function() return data end,Say=function() end,
 getCurrentSquare=function() return {getBuilding=function() return building end} end}
local npc={getWorldObject=function() return body end}
function getCell() return {getGridSquare=function(_,x,y,z)
 squares=squares+1
 if x==0 and y==0 then return {getMovingObjects=function()
  return {size=function() return #scene end,get=function(_,i) return scene[i+1] end}
 end} end
end,getZombieList=function() error('Unbounded global traversal is forbidden') end} end
function getGameTime() return {getWorldAgeHours=function() return 1 end} end
PersonalKnoxAPI={getInstance=function() return {supplyPlan=function() return 'essentials' end} end}
local Supply=require 'PersonalKnox/Supplies'
Supply.install({isMember=function() return true end,say=function() end,journal=function() end})
local function propose()
 Supply.pending[npc]=nil;squares=0;visibility=0
 Supply.propose(player,npc)
 return Supply.pending[npc]~=nil
end
nativeEnemy=true
assert(not propose() and squares==0,'Known enemy NPC must block before a world scan')
nativeEnemy=false;nativeCombat=true
assert(not propose() and squares==0,'Active combat blocks supply runs')
nativeCombat=false;nativeSafe=false
assert(not propose() and squares==0,'Native dangerous targets block supply runs')
nativeSafe=true
assert(propose() and squares==625,'Clear native NPC uses one bounded player-area check')
local zombie={kind='IsoZombie',isDead=body.isDead,getX=body.getX,getY=body.getY}
scene={zombie};visible=false
assert(propose() and visibility==1 and squares<=625,'A hidden zombie is not perceived danger')
visible=true
assert(not propose() and squares==1,'Visible nearby zombie blocks player safety')
local fighter={kind='IsoNpcPlayer',isDead=body.isDead,getX=body.getX,getY=body.getY,isInActiveCombat=function() return true end}
scene={fighter}
assert(not propose() and squares==1,'Visible NPC combat blocks looting even without zombies')
scene={};body.hasNoDangerousTargets=function() error('API unavailable') end
assert(propose() and squares<=1250,'Missing native API falls back without unbounded work')
scene={zombie}
assert(not propose() and squares==1,'Fallback NPC also sees close threats')
scene={}
for i=1,2000 do scene[i]={kind='clutter'} end
assert(not propose() and squares==1 and visibility==0,'Dense unknown scene stops at the object budget')
''')
print('Supply perception checks passed: native enemy/combat state, local visibility, player protection, missing-API fallback, bounded cells/objects, no global zombie scan.')
