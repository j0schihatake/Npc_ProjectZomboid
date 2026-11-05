"""Check one-shot loading guards and readonly NPC probing without opening a real save."""
from pathlib import Path
from lupa.lua51 import LuaRuntime

root=Path(__file__).resolve().parents[1]
lua=LuaRuntime(unpack_returned_tuples=True)
lua.globals().package.path=str(root/'common/media/lua/client/?.lua')
lua.execute('''
local files,events,loaded,quit,reads,now,world,mode,apiCalls={}, {},0,0,0,0,'MainSave','Sandbox',0
local name='PersonalKnox-test-20261105-1-unit'
local nonce=string.rep('a',32)
local logs={}
function print(text) logs[#logs+1]=text end
function getFileReader(path)
    reads=reads+1
    local rows=files[path]
    if not rows then return nil end
    local i=0
    return {readLine=function() i=i+1;return rows[i] end,close=function() end}
end
function getFileWriter(path)
    local text=''
    return {write=function(_,value) text=text..value end,close=function()
        local result={};for row in text:gmatch('[^\\n]+') do result[#result+1]=row end;files[path]=result
    end}
end
function getTimestampMs() return now end
function getWorld() return {getGameMode=function() return mode end,getWorld=function() return world end} end
function setGameSpeed() end
function pauseSoundAndMusic() end
function setShowPausedMessage() end
function getCore() return {quitToDesktop=function() quit=quit+1 end} end
local body={isDead=function() return false end}
local npc={getWorldObject=function() return body end,QueueAction=function() error('Readonly probe must not queue actions') end}
local player={isDead=body.isDead}
function getSpecificPlayer() return player end
PersonalKnoxAPI={getInstance=function() return {npcTransferApi=function(_,actual)
    assert(actual==npc);apiCalls=apiCalls+1;return 'public_api_present'
end} end}
local deps={roster=function() return {npc} end,isMember=function() return true end}
local function resetEvents()
    events={menu={},start={},update={}}
    local function event(key)
        return {Add=function(fn) events[key][#events[key]+1]=fn end,
            Remove=function(fn) for i=#events[key],1,-1 do if events[key][i]==fn then table.remove(events[key],i) end end end}
    end
    Events={OnMainMenuEnter=event('menu'),OnGameStart=event('start'),OnPlayerUpdate=event('update')}
    MainScreen={instance={inGame=false},continueLatestSave=function(gameMode,save)
        assert(gameMode=='Sandbox' and save==name,'Only the exact test copy may be selected')
        loaded=loaded+1;world=save
    end}
end
local function install()
    package.loaded['PersonalKnox/Probe']=nil
    require('PersonalKnox/Probe').install(deps)
end
local function config(state,testWorld,token)
    files['PersonalKnox-probe.txt']={'PersonalKnoxProbe1',testWorld or name,token or nonce,state,'10','1'}
    files['PersonalKnox-probes/'..name..'.txt']={nonce}
end
local function fire(key)
    local copy={};for i,fn in ipairs(events[key]) do copy[i]=fn end
    for _,fn in ipairs(copy) do fn() end
end
resetEvents();install()
assert(#events.menu==0 and #events.start==0 and #events.update==0,'Normal startup must not register test callbacks')
config('armed','../MainSave');install();assert(#events.menu==0,'Reject path traversal')
config('armed',name,string.rep('b',32));install();assert(#events.menu==0,'Reject an unmatched copy token')
config('done');install();assert(#events.menu==0,'A completed run must not reopen a save')
config('armed');install();fire('menu');fire('menu')
assert(loaded==1 and files['PersonalKnox-probe.txt'][4]=='running','Consume before loading and never start twice')
-- Vanilla may reset Lua while switching active mods; model a fresh environment.
resetEvents();install();assert(#events.menu==0,'A running marker must not restart loading after ResetLua')
fire('start');assert(#events.update==1)
now=1000;fire('update');assert(apiCalls==1,'Pass the actual NPC to Java exactly once')
local afterReads=reads
now=1001;fire('update');assert(reads==afterReads,'No file I/O or roster scans between scheduled checks')
now=10000;fire('update')
assert(quit==1 and #events.update==0 and files['PersonalKnox-probe.txt'][4]=='done','Finish and exit only the bound test copy')
config('running');resetEvents();world='MainSave';install();fire('start')
assert(#events.update==0 and quit==1,'Never probe or exit an ordinary world')
config('armed');resetEvents();MainScreen.instance.termsOfServiceDialog={};install();fire('menu')
assert(loaded==1 and files['PersonalKnox-probe.txt'][4]=='blocked','Do not bypass blocking menu dialogs')
config('running');resetEvents();world=name;now=0;install();fire('start')
world='MainSave';now=10000;fire('update')
assert(quit==1 and #events.update==0,'Switching away from the copy must cancel without exiting')
config('running');resetEvents();world=name;now=0;install();fire('start')
files['PersonalKnox-probe.txt'][3]=string.rep('c',32)
now=10000;fire('update')
assert(quit==1 and files['PersonalKnox-probe.txt'][3]==string.rep('c',32),'Do not overwrite or exit a superseded run')
''')
print('Probe guards passed: inert normal startup, path/token validation, one-shot/reset behavior, real adapter NPC argument, throttled I/O, readonly dispatch and no ordinary-world exits.')
