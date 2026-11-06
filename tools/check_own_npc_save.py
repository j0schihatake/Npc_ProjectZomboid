"""Exercise the real Lua save event before the periodic snapshot interval."""
from pathlib import Path
from lupa.lua51 import LuaRuntime
lua=LuaRuntime(unpack_returned_tuples=True)
lua.execute('''
package.preload['PersonalKnox/Settings']=function()return {}end
package.preload['PersonalKnox/Perf']=function()return {}end
package.preload['PersonalKnox/Util']=function()return {}end
savedData={}; current='new-npc'; restored=nil
function isClient()return false end
function getSpecificPlayer()return {getModData=function()return savedData end}end
local api={ownSnapshot=function()return current end,ownPopulationSnapshot=function()return 'population'end,
 ownTargetZSnapshot=function()return 'floor'end,ownRestore=function(_,v)restored=v end,
 ownPopulationRestore=function()end,ownTargetZRestore=function()end}
PersonalKnoxAPI={getInstance=function()return api end}
events={}
local function ev(name)return {Add=function(fn)events[name]=fn end}end
Events={OnGameStart=ev('start'),OnSave=ev('save'),OnPlayerUpdate=ev('tick')}
''')
source=Path(__file__).resolve().parents[1]/'common/media/lua/client/PersonalKnox/OwnNpc.lua'
lua.globals().source=source.read_text()
lua.execute('''
local bridge=assert(loadstring(source))();bridge.install({})
events.save() -- No update ticks, less than 60 seconds since creation.
assert(savedData.PersonalKnoxOwnNpc=='new-npc')
assert(savedData.PersonalKnoxOwnPopulation=='population' and savedData.PersonalKnoxTargetZ=='floor')
current='';events.save();assert(savedData.PersonalKnoxOwnNpc=='','Persist removal of the last NPC')
savedData.PersonalKnoxOwnNpc='saved-npc';events.start();assert(restored=='saved-npc')
current='partial-update'
apiForTest=PersonalKnoxAPI.getInstance()
apiForTest.ownTargetZSnapshot=function()error('fixture snapshot failure')end
events.save();assert(savedData.PersonalKnoxOwnNpc=='saved-npc','Failed snapshot must not partially replace saved data')
function getSpecificPlayer()return nil end
events.save();assert(savedData.PersonalKnoxOwnNpc=='saved-npc','No player must not erase prior data')
''')
print('Own NPC save bridge: immediate save, empty population, restore and missing player passed.')
