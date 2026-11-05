"""Check the real Client roster callback in dense and sparse local scenes."""
from pathlib import Path
import re
from lupa.lua51 import LuaRuntime

root = Path(__file__).resolve().parents[1]
lua = LuaRuntime(unpack_returned_tuples=True)
lua.globals().package.path = str(root / 'common/media/lua/client/?.lua')
for name in re.findall(r'require "(PersonalKnox/[^"]+)"', (root / 'common/media/lua/client/PersonalKnox/Client.lua').read_text()):
    lua.globals().stubName = name
    lua.execute('''
    local name=stubName
    package.preload[name]=function() return {
        install=function(deps) if name=="PersonalKnox/Call" then getRoster=deps.roster end end
    } end
    ''')
lua.execute('''
function getText(key) return key end
Events={OnFillWorldObjectContextMenu={Add=function() end}}
local selected,worker,nearby={name="selected"},{name="worker"},{name="nearby"}
local outsider={name="outsider"}
local group={inGroup=function(_,npc) return npc~=outsider end,
    getSafehouseNpcMembers=function() return {size=function() return 1 end,get=function() return worker end} end}
KnoxEventLoader={getAPI=function() return {getGroup=function() return group end} end}
package.preload.KnoxEventLoader=function() return KnoxEventLoader end
selectedGroupNpcs={selected,worker}
local coordinates,queries,reads=0,0,0
local player={getX=function() coordinates=coordinates+1;return 0 end,
    getY=function() coordinates=coordinates+1;return 0 end,
    getZ=function() coordinates=coordinates+1;return 0 end}
local dense=true
local moving={size=function() return dense and 100000 or 2 end,get=function(_,i)
    reads=reads+1;assert(reads<=512,"Nearby roster must stop after 512 objects")
    if i==0 then return {kind="IsoNpcPlayer",getMetaCharacter=function() return nearby end} end
    if not dense then return {kind="IsoNpcPlayer",getMetaCharacter=function() return outsider end} end
    return {kind="IsoZombie"}
end}
function getCell() return {getGridSquare=function(_,x,y,z)
    queries=queries+1;assert(queries<=289)
    if x==0 and y==0 then return {getMovingObjects=function() return moving end} end
end} end
function instanceof(body,kind) return body.kind==kind end
require "PersonalKnox/Client"
local list=getRoster(player)
assert(#list==3 and list[1]==selected and list[2]==worker and list[3]==nearby,
    "Keep known members, deduplicate native lists and add a nearby follower")
assert(reads==512 and coordinates==3)
dense=false;coordinates=0;queries=0;reads=0
list=getRoster(player)
assert(#list==3 and reads==2 and queries==289 and coordinates==3,
    "Sparse scenes still visit the entire local area; outsiders never join")
''')
print('Roster budget checks passed: 512-object ceiling, 289 squares, cached player coordinates, known members, deduplication and outsider filtering.')
