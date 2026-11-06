"""Exercise the real launcher-only Lua scenario, including paused Knox setup."""
from pathlib import Path
from lupa.lua51 import LuaRuntime
script=Path(__file__).with_name('dev_scenario_mod')/'common/media/lua/client/PersonalKnoxDevScenario.lua'
lua=LuaRuntime(unpack_returned_tuples=True)
lua.execute('''
files={};events={};now=0;world='menu';loaded=0;generated=0;exported=0;saved=0;ready=false;speed=0
local function event(k)
 events[k]={};return {Add=function(fn)table.insert(events[k],fn)end,Remove=function(fn)
 for i=#events[k],1,-1 do if events[k][i]==fn then table.remove(events[k],i)end end end}
end
Events={OnMainMenuEnter=event('menu'),OnGameStart=event('start'),OnTick=event('tick'),OnTickEvenPaused=event('paused')}
function fire(k)local copy={};for i,f in ipairs(events[k])do copy[i]=f end;for _,f in ipairs(copy)do f()end end
function getFileReader(p)local s=files[p];if not s then return end;local rows={};for r in s:gmatch('[^\\n]+')do table.insert(rows,r)end
 local i=0;return {readLine=function()i=i+1;return rows[i]end,close=function()end}end
function getFileWriter(p)local s='';return {write=function(_,v)s=s..v end,close=function()files[p]=s end}end
function getWorld()return {getGameMode=function()return 'Sandbox'end,getWorld=function()return world end}end
function getTimestampMs()return now end
function setGameSpeed(n)speed=n end
function saveGame()saved=saved+1 end
function getSpecificPlayer()return {isDead=function()return false end}end
function panel()
 return {visible=true,isVisible=function(self)return self.visible end,
 setVisible=function(self,value)self.visible=value end}
end
inventories={panel(),panel()};loots={panel(),panel()}
for _,p in ipairs(inventories)do p.infoRichText=panel()end
function getNumActivePlayers()return 2 end
function getPlayerInventory(n)return inventories[n+1]end
function getPlayerLoot(n)return loots[n+1]end
SurvivalGuide={instance=panel()}
function SurvivalGuide.instance:onClose()self.visible=false;setGameSpeed(1)end
errorDialog=panel()
PersonalKnoxAPI={getInstance=function()return {}end}
MainScreen={instance={},continueLatestSave=function(mode,name)assert(mode=='Sandbox');loaded=loaded+1;world=name end}
local api={isGenerated=function()return ready end,getSurvivorCount=function()return 12 end}
KnoxEventLoader={getAPI=function(k)assert(k=='procGen');return api end}
function knoxUI()
 NpcProcGenWorldMap_instance={controlWindow={doGenerate=function()generated=generated+1 end},close=function()
 exported=exported+1;NpcProcGenWorldMap_instance=nil;setGameSpeed(1)end}
end
function arm()
 nonce=string.rep('a',32);name='PersonalKnox-scenario-'..nonce
 files['PersonalKnox-dev-scenario.txt']='PersonalKnoxDevScenario1\\n'..name..'\\n'..nonce..'\\n10\\narmed\\n'
end
''')
lua.execute(script.read_text());assert len(lua.globals().events.menu)==0
lua.execute('arm()');lua.execute(script.read_text())
lua.execute('''
fire('menu');fire('menu');assert(loaded==1,'Load exactly once')
fire('start');knoxUI();fire('paused');assert(generated==1 and exported==0)
for _,p in ipairs(inventories)do assert(not p.visible and not p.infoRichText.visible)end
for _,p in ipairs(loots)do assert(not p.visible)end
assert(not SurvivalGuide.instance.visible and errorDialog.visible,'Close welcome, preserve error dialogs')
assert(NpcProcGenWorldMap_instance,'Do not hide Knox generation UI before exporting')
-- A delayed welcome window must also close while the game is paused.
SurvivalGuide.instance.visible=true
now=1000;fire('paused');assert(generated==1,'Do not generate twice')
assert(not SurvivalGuide.instance.visible)
now=2000;ready=true;fire('paused');assert(exported==1 and speed==1)
now=3000;fire('tick');assert(saved==0)
now=11000;fire('tick');assert(saved==1)
assert(files['PersonalKnox-dev-scenario-result.json']:find('"status":"OK"',1,true))
fire('tick');assert(saved==1,'One shot completion')
inventories[1].visible=true;SurvivalGuide.instance.visible=true
now=12000;fire('paused')
assert(inventories[1].visible and SurvivalGuide.instance.visible,'After completion leave manually opened windows alone')
''')
lua.execute('''
arm()
files['PersonalKnox-dev-scenario.txt']=files['PersonalKnox-dev-scenario.txt']:gsub(string.rep('a',32),string.rep('b',32))
world='menu';ready=false
inventories[1].setVisible=function()error('simulated UI failure')end
''')
lua.execute(script.read_text())
lua.execute('''
fire('menu');fire('start');knoxUI();fire('paused')
assert(files['PersonalKnox-dev-scenario-result.json']:find('startup_ui_cleanup_failed',1,true))
assert(generated==1,'Cleanup failure must stop further generation actions')
now=30000;fire('tick');fire('paused');assert(saved==1,'A failed scenario must not turn into a later OK')
''')
print('Lua scenario passed: one-shot load, paused Knox setup, inventory/welcome cleanup, preserved error dialogs, late welcome, no cleanup after OK and terminal cleanup errors.')
