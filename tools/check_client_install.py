"""Check the real Client bootstrap installs handlers once and one F10 makes one spawn."""
import argparse
from pathlib import Path
import re
from lupa.lua51 import LuaRuntime

root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--client',type=Path,default=root/'common/media/lua/client/PersonalKnox/Client.lua')
args=parser.parse_args()
lua=LuaRuntime(unpack_returned_tuples=True)
lua.globals().clientSource=args.client.read_text()
lua.globals().debugSource=(root/'common/media/lua/client/PersonalKnox/OwnNpcDebug.lua').read_text()
lua.execute('counts={};keys={};package.preload.KnoxEventLoader=function() return {} end')
for name in re.findall(r'require "(PersonalKnox/[^"]+)"',args.client.read_text()):
    lua.globals().stubName=name
    lua.execute('''
    local name=stubName
    package.preload[name]=function()
        local module={loaded='fixture'}
        if name=='PersonalKnox/OwnNpcDebug' then module=assert(loadstring(debugSource))() end
        local original=module.install
        module.install=function(...)
            counts[name]=(counts[name] or 0)+1
            if original then original(...) end
        end
        return module
    end
    ''')
lua.execute('''
Events={OnFillWorldObjectContextMenu={Add=function() end},OnKeyPressed={Add=function(fn) keys[#keys+1]=fn end}}
function getText(key) return key end
function isClient() return false end
function getCore() return {isDoingTextEntry=function() return false end} end
function getSpecificPlayer() return {isDead=function() return false end,getX=function() return 0 end,
    getY=function() return 0 end,getZ=function() return 0 end} end
Keyboard={KEY_F10=121}
local spawned=0
PersonalKnoxAPI={getInstance=function() return {
    ownCreateCompanion=function(_,player) assert(player and not player:isDead());spawned=spawned+1;return 'debug'..spawned end,
    ownDebugBodySync=function() return spawned end,
} end}
assert(loadstring(clientSource))()
for _,fn in ipairs(keys) do fn(Keyboard.KEY_F10) end
assert(spawned==1,'One F10 must spawn exactly one NPC')
for name,count in pairs(counts) do assert(count==1,'Duplicate module install: '..name) end
''')
print('Client bootstrap checks passed: each module installs once, the actual F10 handler creates one NPC.')
