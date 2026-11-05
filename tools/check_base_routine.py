"""Check native schedule integration, undo and rollback without the game."""
from pathlib import Path
from lupa.lua51 import LuaRuntime

root = Path(__file__).resolve().parents[1]
lua = LuaRuntime(unpack_returned_tuples=True)
lua.globals().package.path = str(root/'common/media/lua/client/?.lua')
lua.execute('''
function getText(key) return key end
function isClient() return false end
NpcSchedule={ANY={},WORK={},RECREATION={},REST={},GUARD={}}
local schedule={}
for h=0,23 do schedule[h]=NpcSchedule.ANY end
schedule[8]=NpcSchedule.GUARD
local failHour=nil
local priorities={getScheduleForHour=function(_,h) return schedule[h] end,
 setScheduleForHour=function(_,h,v)
  if h==failHour then failHour=nil; error("simulated native failure") end
  schedule[h]=v
 end}
local npc={getId=function() return 12 end,getJobPrioritizer=function() return priorities end}
local data={}
local player={getModData=function() return data end,Say=function() end}
local home=true
local returned=0
KnoxEventLoader={getAPI=function() return {
 getPlayerSafehouse=function() return home and {} or nil end,
 getGroup=function() return {sendToSafehouse=function(_,target) assert(target==npc);returned=returned+1 end} end
} end}
PersonalKnoxAPI={getInstance=function() return {baseSchedule=function(_,h,current)
 if current=="GUARD" then return "GUARD" end
 return "REST"
end} end}
local member=true
local Routine=require "PersonalKnox/BaseRoutine"
Routine.install({isMember=function() return member end})
Routine.apply(player,npc)
assert(schedule[0]==NpcSchedule.REST and schedule[8]==NpcSchedule.GUARD)
assert(returned==0,"Changing schedule must not send a follower home")
Routine.apply(player,npc)
Routine.restore(player,npc)
assert(schedule[0]==NpcSchedule.ANY and schedule[8]==NpcSchedule.GUARD,"Reapply must preserve original undo")
failHour=12
Routine.apply(player,npc)
assert(schedule[0]==NpcSchedule.ANY and schedule[23]==NpcSchedule.ANY,"Partial change must roll back")
assert(data.PersonalKnox.baseSchedules['12']==nil,"Failed apply must not save an undo")
schedule[23]={}
Routine.apply(player,npc)
assert(schedule[0]==NpcSchedule.ANY,"Unknown native enum must block all writes")
schedule[23]=NpcSchedule.ANY
member=false
Routine.apply(player,npc);Routine.returnHome(player,npc)
assert(returned==0 and schedule[0]==NpcSchedule.ANY)
member=true;home=false
Routine.apply(player,npc);Routine.returnHome(player,npc)
assert(returned==0 and schedule[0]==NpcSchedule.ANY)
home=true
Routine.returnHome(player,npc)
assert(returned==1)
Routine.apply(player,npc)
data.PersonalKnox.baseSchedules['12'][5]='BOGUS'
Routine.restore(player,npc)
assert(schedule[0]==NpcSchedule.REST,"Corrupt backup must block restore without partial changes")
''')
print('Base routine checks passed: guard preservation, no movement on apply, undo, rollback, unknown enums, membership, safehouse.')
