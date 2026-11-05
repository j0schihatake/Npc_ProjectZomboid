"""Check explicit recall and cancellation using Knox's installed Lua follow command."""
from pathlib import Path
from lupa.lua51 import LuaRuntime

root = Path(__file__).resolve().parents[1]
lua = LuaRuntime(unpack_returned_tuples=True)
lua.globals().package.path = str(root/'common/media/lua/client/?.lua')
source = Path('/home/j0schi/SteamLibrary/steamapps/workshop/content/108600/3397396843/mods/KnoxEventExpanded/42/media/lua/client/01_Core/SPCommands.lua')
lua.globals().nativeCommands = source.read_text()
lua.globals().radialSource = (source.parents[1]/'ISUI/ISNpcGroupRadialMenu.lua').read_text()
lua.execute('''
KnoxEventLoader={defer=function() end}
package.preload.KnoxEventLoader=function() return KnoxEventLoader end
package.preload['TimedActions/ISReloadWeaponAction']=function() return {} end
assert(loadstring(nativeCommands))()
function getText(key,...) return key end
local client=false
function isClient() return client end
classFilesInstalled=true
local members={}
function instanceof(object,kind) return members[object]~=nil and kind=='MetaNpcCharacter' end
local player={isDead=function() return false end,getX=function() return 0 end,getY=function() return 0 end,Say=function() end}
local cancelled, transferred, stopped, logged=0,0,0,0
local group={sendToGroup=function(_,npc) assert(members[npc]);transferred=transferred+1 end}
KnoxEventLoader={getAPI=function() return {getGroup=function() return group end} end}
local function npcAt(x)
 local npc={goal='SafehouseSubTree',subGoal='BarricadeSubTree'}
 local body={isDead=function() return false end,getX=function() return x end,getY=function() return 0 end,
  StopAllActionQueue=function() stopped=stopped+1 end}
 npc.getWorldObject=function() return body end
 npc.getFullName=function() return 'Companion' end
 npc.setGoal=function(_,goal) npc.goal=goal end
 npc.setSubGoal=function(_,goal) npc.subGoal=goal end
 members[npc]=true
 return npc
end
local nearby,far=npcAt(5),npcAt(31)
local roster={nearby,nearby,far}
Events={OnKeyPressed={Add=function(fn) onKey=fn end}}
local typing=false
function getCore() return {isDoingTextEntry=function() return typing end,isKey=function(_,name,key) return name=='PersonalKnoxRecall' and key==99 end} end
function getSpecificPlayer() return player end
local Call=require 'PersonalKnox/Call'
Call.install({isMember=function(_,npc) return members[npc]==true end,
 roster=function() return roster end,journal=function() logged=logged+1 end,
 cancelTasks=function() cancelled=cancelled+1 end})
assert(Call.one(player,nearby))
assert(nearby.goal=='RoamingSubTree' and nearby.subGoal=='FollowSubTree',"Installed Knox command must actually switch behaviour")
assert(cancelled==1 and stopped==1 and transferred==1 and logged==1)
assert(not Call.one(player,far),"Voice recall is local, not an unlimited remote order")
members[nearby]=false
assert(not Call.one(player,nearby) and cancelled==1,"Never command non-members")
members[nearby]=true
Call.all(player)
assert(cancelled==2,"Group recall must deduplicate targets")
typing=true;onKey(99);assert(cancelled==2,"Typing must not recall the squad")
typing=false;onKey(20);assert(cancelled==2,"T/chat stays unchanged")
onKey(99);assert(cancelled==3)
client=true;onKey(99);assert(cancelled==3,"SP only")
client=false
local unloaded=npcAt(1);unloaded.getWorldObject=function() return nil end
assert(not Call.one(player,unloaded))
roster={}
for i=1,20 do roster[i]=npcAt(1) end
Call.all(player);assert(cancelled==19,"At most 16 successful recalls per invocation")
local before=cancelled
group=nil
assert(not Call.one(player,nearby) and cancelled==before,"Missing group must block before cancellation")
-- Weapon entries delegate to installed Knox preferences for the entire squad.
package.preload['ISUI/ISRadialMenu']=function() return {} end
ISRadialMenu={}
ISBaseObject={derive=function(self) return setmetatable({},{__index=self}) end}
Events.OnGameStart={Add=function() end}
assert(loadstring(radialSource))()
local preference=nil
local squad={setWeaponPreference=function(_,value) preference=value end}
KnoxEventLoader.getAPI=function() return {getSquad=function() return squad end} end
WeaponPreference={MELEE={},RANGED={},ANY={}}
local Weapons=require 'PersonalKnox/Weapons'
Weapons.install({isMember=function(_,npc) return members[npc]==true end})
local menu={options={}}
function menu:addOption(label,target,fn,a,b) self.options[#self.options+1]={target=target,fn=fn,a=a,b=b} end
Weapons.addOptions(menu,player,nearby)
assert(#menu.options==3)
for index,value in ipairs({WeaponPreference.MELEE,WeaponPreference.RANGED,WeaponPreference.ANY}) do
 local option=menu.options[index]
 option.fn(option.target,option.a,option.b)
 assert(preference==value,"Installed Knox preference must change")
end
members[nearby]=false
local option=menu.options[1]
option.fn(option.target,option.a,option.b)
assert(preference==WeaponPreference.ANY,"Stale weapon entry must recheck membership")
''')
print('Recall and weapon checks passed with installed native commands: task cancellation, membership, distance, typing, key mapping, SP guard, batch limit, squad-wide melee/ranged/any preference.')
