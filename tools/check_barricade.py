"""Check the native-job barricade adapter: bounded supplies, cancellation and honest completion."""
from pathlib import Path
from lupa.lua51 import LuaRuntime

root=Path(__file__).resolve().parents[1]
lua=LuaRuntime(unpack_returned_tuples=True)
lua.globals().package.path=str(root/'common/media/lua/client/?.lua')
lua.execute('''
local now,member,unloaded,allowNode=0,true,false,true
local visible,dangerObjects=false,{}
local granted,followed,stopped,transferred,announced=0,0,0,0,0
local messages={}
function getText(key) return key end
function isClient() return false end
classFilesInstalled=true
Events={EveryOneMinute={Add=function(fn) tick=fn end},OnGameStart={Add=function(fn) reset=fn end},OnKeyPressed={Add=function() end}}
ItemTag={HAMMER={}}
CharacterStat={HUNGER={},THIRST={},FATIGUE={}}
local function list(items)
 return {size=function() return #items end,get=function(_,index) return items[index+1] end}
end
local square
local function window(planks,opposite)
 local w={kind='IsoWindow',planks=planks,valid=true,metal=false}
 local side={getNumPlanks=function() return w.planks end,isMetal=function() return w.metal end,isMetalBar=function() return false end}
 w.getBarricadeOnSameSquare=function() return not opposite and side or nil end
 w.getBarricadeOnOppositeSquare=function() return opposite and side or nil end
 w.isBarricadeAllowed=function() return true end
 w.getObjectIndex=function() return w.valid and 0 or -1 end
 w.getSquare=function() return w.valid and {} or nil end
 w.getIndoorSquare=function() return square end
 return w
end
local w1,w2=window(1,false),window(1,true)
local foreign=window(0,false)
foreign.getIndoorSquare=function() return {getBuilding=function() return {getDef=function() return {equals=function() return false end} end} end} end
local windows={w1,w2,foreign}
-- Reproduce the real house: cached room windows empty, world windows on exterior squares.
local room={getWindows=function() return list({}) end}
local maxX=0
local def={getRooms=function() return list({{getIsoRoom=function() return not unloaded and room or nil end,getZ=function() return 0 end}}) end,
 getX=function() return 0 end,getY=function() return 0 end,getX2=function() return maxX end,getY2=function() return 0 end}
function def:equals(other) return other==self end
local building={getDef=function() return def end}
square={getBuilding=function() return building end}
local stock={['Base.Plank']=2,['Base.Nails']=3}
local hammers={{isBroken=function() return true end}}
local inventory={getItemCount=function(_,name,recurse) assert(recurse);return stock[name] or 0 end,
 getFirstTagEvalRecurse=function(_,tag,predicate)
  assert(tag==ItemTag.HAMMER)
  for _,hammer in ipairs(hammers) do if predicate(hammer) then return hammer end end
 end,
 AddItem=function(_,name)
  granted=granted+1
  if name=='Base.Hammer' then hammers[#hammers+1]={isBroken=function() return false end}
  else stock[name]=(stock[name] or 0)+1 end
  return {}
 end}
local body={getX=function() return 0 end,getY=function() return 0 end,getZ=function() return 0 end,
 getInventory=function() return inventory end,getCurrentSquare=function() return square end,
 isDead=function() return false end,CanSee=function() return visible end,
 StopAllActionQueue=function() stopped=stopped+1 end}
body.getBodyDamage=function() return {getHealth=function() return 80 end} end
body.getStats=function() return {get=function(_,stat)
 assert(stat==CharacterStat.HUNGER or stat==CharacterStat.THIRST or stat==CharacterStat.FATIGUE)
 return 0.4
end} end
local player={getCurrentSquare=function() return square end,getX=body.getX,getY=body.getY,
 isDead=body.isDead,Say=function(_,text) messages[#messages+1]=text end}
local counter=0
local function npc()
 counter=counter+1;local id=counter
 local n={kind='MetaNpcCharacter',goal='RoamingSubTree',subGoal='FollowSubTree'}
 n.getId=function() return id end
 n.getFullName=function() return 'Companion' end
 n.getWorldObject=function() return body end
 n.getGoal=function() return n.goal end;n.getSubGoal=function() return n.subGoal end
 n.setGoal=function(_,v) n.goal=v end;n.setSubGoal=function(_,v) n.subGoal=v end
 n.getBehaviorTree=function() return {
  getChildWithName=function(_,name) assert(name=='BarricadeSubTree');return nil end,
  getGoalNode=function() return {getChildWithNameWithDepth=function(_,name,depth)
   assert(depth==1)
   if name=='SafehouseSubTree' and n.goal=='SafehouseSubTree' then
    return {getChildWithName=function(_,child) assert(child=='BarricadeSubTree');return allowNode and {} or nil end}
   end
  end} end
 } end
 return n
end
function instanceof(obj,kind) return obj.kind==kind end
function tellNpcFollow(_,n) followed=followed+1;n.goal='RoamingSubTree';n.subGoal='FollowSubTree' end
local safehouse=def
local group={sendToSafehouse=function() transferred=transferred+1 end,sendToGroup=function() transferred=transferred+1 end}
KnoxEventLoader={getAPI=function() return {getPlayerSafehouse=function() return safehouse end,getGroup=function() return group end} end}
function getGameTime() return {getWorldAgeHours=function() return now end} end
local reads=0
function getCell() return {getGridSquare=function(_,x,y,z)
 reads=reads+1
 return {getMovingObjects=function() return list(x==0 and y==0 and dangerObjects or {}) end,
  getObjects=function() return list(x==-1 and y==0 and windows or {}) end}
end,getZombieList=function() error('No whole-world zombie scan allowed') end} end
PersonalKnoxAPI={getInstance=function() return {
 barricadeMissing=function(_,planks,metal) assert(planks>=0 and planks<=4);return metal and 0 or 4-planks end,
 barricadeStockTarget=function(_,missing,nails) return math.min(4,missing)*(nails and 2 or 1) end,
 barricadeStatus=function(_,missing,owns,danger,idle)
  if not owns then return 'interrupted' elseif danger then return 'danger' elseif missing==0 then return 'done'
  elseif idle>=2 then return 'stalled' else return 'work' end
 end,
} end}
local Barricade=require 'PersonalKnox/Barricade'
Barricade.install({isMember=function() return member end,journal=function() end,
 say=function(_,_,topic) assert(topic=='plan_barricade');announced=announced+1 end,cancelSupply=function() end})
local n=npc()
local report=Barricade.inspect(player,n)
assert(report:find('hunger=0.4',1,true) and report:find('nativeBarricadeCurrent=false',1,true) and report:find('windows=2',1,true))
assert(granted==0 and stopped==0 and transferred==0 and #Barricade.active==0,'Inspection must not issue orders, grant materials or interrupt actions')
Barricade.start(player,n)
assert(#Barricade.active==1 and #Barricade.active[1].windows==2 and stock['Base.Plank']==4 and stock['Base.Nails']==8 and #hammers==2)
assert(announced==1 and n.subGoal=='BarricadeSubTree')
local before=granted
tick();tick();assert(granted==before,'Never accumulate unused materials or duplicate healthy tools')
stock['Base.Plank']=0;stock['Base.Nails']=0;w1.planks=4
now=1.5;tick()
assert(stock['Base.Plank']==3 and stock['Base.Nails']==6 and #hammers==2,'Refill only remaining work')
now=3;tick();assert(#Barricade.active==1,'Recent progress resets stall timer')
w2.planks=4;before=granted;tick()
assert(#Barricade.active==0 and n.subGoal=='FollowSubTree' and granted==before)
assert(messages[#messages]=='ContextMenu_PersonalKnox_BarricadeDone')
-- Starting a new job respects the new state of each window and the native order.
w1.planks=0;now=4
Barricade.start(player,n);before=granted
n.goal='LootingSubTree';n.subGoal='NewOrder';local stopBefore=stopped
tick();assert(#Barricade.active==0 and granted==before and stopped==stopBefore and n.subGoal=='NewOrder')
-- Calling the NPC cancels this job before native follow is dispatched.
n.goal='RoamingSubTree';n.subGoal='FollowSubTree'
Barricade.start(player,n)
local Call=require 'PersonalKnox/Call'
Call.install({isMember=function() return member end,roster=function() return {n} end,journal=function() end,cancelTasks=Barricade.cancel})
assert(Call.one(player,n,true));before=granted;tick()
assert(#Barricade.active==0 and n.subGoal=='FollowSubTree' and granted==before)
-- A pause does not advance game hours; a genuine stall stops without claiming success.
Barricade.start(player,n);before=granted;tick();assert(#Barricade.active==1 and granted==before)
now=6;tick();assert(#Barricade.active==0 and messages[#messages]=='ContextMenu_PersonalKnox_BarricadeStalled')
-- Completion must be rechecked against newly created windows.
now=7;Barricade.start(player,n)
w1.planks=4;local newWindow=window(0,false);windows[3]=newWindow
tick();assert(#Barricade.active==1,'New windows must prevent false completion')
newWindow.metal=true;tick();assert(#Barricade.active==0,'Existing metal protection needs no planks')
-- An unknown/unloaded target cannot silently count as done.
w1.planks=0;Barricade.start(player,n);w1.valid=false;tick()
assert(#Barricade.active==0 and messages[#messages]~='ContextMenu_PersonalKnox_BarricadeDone')
w1.valid=true
-- Reject invalid geometry before changing tasks; roll back a missing native branch after assignment.
before=granted;unloaded=true;Barricade.start(player,n);unloaded=false
assert(#Barricade.active==0 and granted==before)
allowNode=false;Barricade.start(player,n);allowNode=true
assert(#Barricade.active==0 and granted==before)
assert(n.goal=='RoamingSubTree' and n.subGoal=='FollowSubTree','Unsupported lazy native assignment must roll back')
maxX=10000;local beforeBounds=reads;Barricade.start(player,n);maxX=0
assert(#Barricade.active==0 and granted==before and reads==beforeBounds,'Reject oversized geometry before scanning cells')
safehouse={equals=function() return false end};Barricade.start(player,n);safehouse=def
assert(#Barricade.active==0 and granted==before)
local zombie={kind='IsoZombie',isDead=function() return false end}
dangerObjects={zombie};visible=false;Barricade.start(player,n)
assert(#Barricade.active==1,'Unseen zombie must not trigger omniscient detection')
visible=true;reads=0;tick()
assert(#Barricade.active==0 and reads<=49 and n.subGoal=='FollowSubTree','Danger interrupts job and returns to squad')
visible=false;dangerObjects={};reset()
for i=1,9 do Barricade.start(player,npc()) end
assert(#Barricade.active==8,'Bound simultaneous jobs')
now=now+2;reads=0;tick()
assert(#Barricade.active==4 and reads<=49*4,'Process at most four jobs per poll')
tick();assert(#Barricade.active==0)
''')
print('Barricade adapter checks passed: bounded refill, existing protection, completion rescan, interruption/call, stall clock, unloaded targets, danger perception, task/poll budgets. Native job execution still requires the game.')
