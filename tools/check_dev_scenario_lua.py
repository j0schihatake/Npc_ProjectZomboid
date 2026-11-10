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
Events={OnMainMenuEnter=event('menu'),OnGameStart=event('start'),OnTick=event('tick'),OnTickEvenPaused=event('paused'),OnRenderTick=event('render')}
function fire(k)local copy={};for i,f in ipairs(events[k])do copy[i]=f end;for _,f in ipairs(copy)do f()end end
function getFileReader(p)local s=files[p];if not s then return end;local rows={};for r in s:gmatch('[^\\n]+')do table.insert(rows,r)end
 local i=0;return {readLine=function()i=i+1;return rows[i]end,close=function()end}end
function getFileWriter(p)local s='';return {write=function(_,v)s=s..v end,close=function()files[p]=s end}end
GameMode={SANDBOX='Sandbox'}
function getWorld()return {getGameMode=function()return 'Sandbox'end,getWorld=function()return world end,setGameMode=function(self,mode)gameMode=mode end,setWorld=function(self,name)world=name end}end
function getTimestampMs()return now end
function setGameSpeed(n)speed=n end
function getGameSpeed()return speed end
function saveGame()saved=saved+1 end
modData={}
function getSpecificPlayer()return {isDead=function()return false end,getModData=function()return modData end}end
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
MainScreen={instance={},continueLatestSave=function(mode,name)assert(mode=='Sandbox');loaded=loaded+1;world=name end,continueLatestSaveAux=function(mode,second)loaded=loaded+1 end}
function MainScreen.instance:setDefaultSandboxVars()end
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
assert(speed==1,'Paused dev load must auto-unpause')
for _,p in ipairs(inventories)do assert(not p.visible and not p.infoRichText.visible)end
for _,p in ipairs(loots)do assert(not p.visible)end
assert(not SurvivalGuide.instance.visible and errorDialog.visible,'Close welcome, preserve error dialogs')
assert(NpcProcGenWorldMap_instance,'Do not hide Knox generation UI before exporting')
-- A delayed welcome window must also close while the game is paused.
SurvivalGuide.instance.visible=true
now=1000;fire('paused');assert(generated==1,'Do not generate twice')
assert(not SurvivalGuide.instance.visible)
now=2000;ready=true;fire('paused');assert(exported==1 and speed==1)
now=3000;fire('paused');assert(saved==0)
now=11000;fire('paused');assert(saved==1)
assert(files['PersonalKnox-dev-scenario-result.json']:find('"status":"OK"',1,true))
fire('paused');assert(saved==1,'One shot completion')
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
now=30000;fire('paused');assert(saved==1,'A failed scenario must not turn into a later OK')
''')
print('Lua scenario passed: one-shot load, paused Knox setup, inventory/welcome cleanup, preserved error dialogs, late welcome, no cleanup after OK and terminal cleanup errors.')

# The dedicated scenario must create a real-body request and refuse missing bodies.
def npc_case(token, succeeds, restore=False, empty=False, active=2):
    lua.globals().testToken=token
    lua.execute('''
    now=0;world='menu';NpcProcGenWorldMap_instance=nil
    local token=string.rep(testToken,32)
    files['PersonalKnox-dev-scenario.txt']='PersonalKnoxDevScenario1\\nPersonalKnox-scenario-'..token..'\\n'..token..'\\n10\\narmed\\n'
    files['PersonalKnox-dev-npc.txt']=token..'\\n'
    inventories={panel(),panel()};loots={panel(),panel()}
    createdBodies=0;bodyReady=false
    PersonalKnoxAPI={getInstance=function()return {
      ownCreateCompanion=function()createdBodies=createdBodies+1;return 'debug#1'end,
      ownBodyReady=function()return bodyReady end,
      ownSnapshot=function()return 'npc-snapshot'end,
      ownPopulationSnapshot=function()return 'population-snapshot'end,
      ownTargetZSnapshot=function()return 'z-snapshot'end,
      ownBodyInspect=function()return 'id=debug#1 ready='..tostring(bodyReady)end,
    }end}
    ''')
    lua.globals().restoreActive=active
    if restore:
        lua.execute("files['PersonalKnox-dev-npc.txt']=files['PersonalKnox-dev-npc.txt']..'own-restore\\n';modData={PersonalKnoxOwnNpc='first|saved\\nsecond|saved\\n'}")
        lua.execute("local api=PersonalKnoxAPI.getInstance();api.ownActive=function()return restoreActive end;api.ownBodiesSync=function()return 2 end;PersonalKnoxAPI.getInstance=function()return api end")
        if empty:
            lua.execute("modData.PersonalKnoxOwnNpc=''")
    lua.execute(script.read_text())
    lua.execute("fire('menu');fire('start');fire('paused');now=100;fire('paused')")
    expected=0 if restore else 1
    assert lua.globals().createdBodies==expected
    if empty or (restore and active!=2):
        lua.execute("assert(files['PersonalKnox-dev-scenario-result.json']:find('own_npc_spawn_failed',1,true))")
        return
    if succeeds:
        lua.execute("bodyReady=true;now=11000;fire('paused');fire('paused');fire('paused');assert(modData.PersonalKnoxOwnNpc=='npc-snapshot');assert(files['PersonalKnox-dev-scenario-result.json']:find('model_saved',1,true))")
    else:
        lua.execute("now=20000;fire('paused');assert(files['PersonalKnox-dev-scenario-result.json']:find('own_npc_spawn_failed',1,true))")
    assert lua.globals().createdBodies==expected, 'Restore completion must never spawn a replacement'
npc_case('c',True)
npc_case('d',False)
npc_case('e',True,restore=True)
npc_case('f',False,restore=True,empty=True)
npc_case('0',False,restore=True,active=3)
print('Own NPC scenario passed: creates once, waits for registered/model-ready body and rejects absent bodies.')
