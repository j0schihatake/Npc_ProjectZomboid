"""Exercise the actual Lua-to-Java follow call on an upper floor."""
from pathlib import Path
from lupa.lua51 import LuaRuntime
lua=LuaRuntime(unpack_returned_tuples=True)
lua.execute('''
callbacks={};errors={};followCalls=0
package.preload['PersonalKnox/Settings']=function()return {diagnosticsEnabled=function()return false end}end
package.preload['PersonalKnox/Perf']=function()return {skip=function()return false end}end
package.preload['PersonalKnox/Util']=function()return {}end
function print(text)if text:find('error:',1,true)then table.insert(errors,text)end end
function isClient()return false end
function getTimestampMs()return 10000 end
function getAverageFPS()return 60 end
local player={isDead=function()return false end,getX=function()return 10.5 end,getY=function()return 20.5 end,getZ=function()return 2 end,getModData=function()return {}end}
function getSpecificPlayer()return player end
local api={knoxGroupMembers=function()return 0 end,ownGroup=function()end,ownSync=function()end,
 ownTick=function()end,ownActive=function()return 0 end,ownBodiesSync=function()return 0 end,
 knoxMemberSnapshot=function()return ''end,knoxStatus=function()return 'ok'end,
 ownFollowTarget3=function(_,x,y,z)
  assert(type(z)=='number','Java primitive double must not receive nil')
  assert(x==10.5 and y==20.5 and z==2,'Forward the actual player floor')
  followCalls=followCalls+1
 end}
PersonalKnoxAPI={getInstance=function()return api end}
Events={OnGameStart={Add=function()end},OnPlayerUpdate={Add=function(fn)table.insert(callbacks,fn)end}}
''')
source=Path(__file__).resolve().parents[1]/'common/media/lua/client/PersonalKnox/OwnNpc.lua'
module=lua.execute(source.read_text());module.install(lua.table_from({'roster':lambda _:lua.table()}))
lua.execute("for _,callback in ipairs(callbacks)do callback()end;assert(followCalls==1,'Follow call failed: '..table.concat(errors,'; '));assert(#errors==0)")
print('Own-NPC bridge passed: actual player XYZ reaches the Java follow method without nil/error.')
