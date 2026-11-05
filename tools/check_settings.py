"""Use the installed B42 ModOptions implementation to check persistence and diagnostic gating."""
from pathlib import Path
from lupa.lua51 import LuaRuntime

root = Path(__file__).resolve().parents[1]
native = Path('/home/j0schi/SteamLibrary/steamapps/common/ProjectZomboid/projectzomboid/media/lua/client/PZAPI/ModOptions.lua')
lua = LuaRuntime(unpack_returned_tuples=True)
lua.globals().package.path = str(root / 'common/media/lua/client/?.lua')
lua.globals().nativeOptionsSource = native.read_text()
lua.execute('''
function getText(key) return key end
package.preload['PZAPI/ModOptions']=function() assert(loadstring(nativeOptionsSource))();return PZAPI.ModOptions end
local settings=require 'PersonalKnox/Settings'
local options=PZAPI.ModOptions:getOptions('PersonalKnox')
assert(#PZAPI.ModOptions.Data==1 and #options.data==12,'Twelve PersonalKnox options')
assert(settings.speechEnabled() and not settings.diagnosticsEnabled(),'Speech on, diagnostics off by default')
assert(settings.autoMedic() and settings.autoDrink() and settings.autoEat(),'Self-care on by default')
assert(settings.infiniteDurability() and settings.infiniteAmmo() and settings.autoUnstick() and settings.careSearchBags() and settings.helpFrequency()==3 and settings.npcVoices() and settings.npcNeedRate()==3,'Upkeep, unstick, bag search, voices, need rate and help frequency defaults')
local lines={}
function getFileWriter()
 return {write=function(_,text) lines[#lines+1]=text:gsub('[\\r\\n]+$','') end,close=function() end}
end
function getFileReader()
 local i=0
 return {readLine=function() i=i+1;return lines[i] end,close=function() end}
end
luautils={split=function(text,separator)
 local parts={};for part in text:gmatch('[^'..separator..']+') do parts[#parts+1]=part end;return parts
end}
options:getOption('speech'):setValue(false)
options:getOption('diagnostics'):setValue(true)
PZAPI.ModOptions:save()
options:getOption('speech'):setValue(true)
options:getOption('diagnostics'):setValue(false)
PZAPI.ModOptions:load()
assert(not settings.speechEnabled() and settings.diagnosticsEnabled(),'Settings survive native save/load')
options:getOption('diagnostics'):setValue(false)
function isClient() return false end
local barricade=require 'PersonalKnox/Barricade'
local labels={}
local menu={addOption=function(_,name) labels[#labels+1]=name end}
barricade.addOption(menu,{}, {})
assert(#labels==1 and labels[1]=='ContextMenu_PersonalKnox_BarricadeHome','Keep the real command, hide diagnostics')
assert(barricade.inspect({}, {})==nil,'A stale diagnostic action must be inert after disabling diagnostics')
-- Disabling speech must skip all world reads, even with reactions enabled in the save.
classFilesInstalled=true;PersonalKnoxAPI={}
function getSpecificPlayer() return {isDead=function() return false end} end
function getCell() error('Speech off must not scan the world') end
local reactions=require 'PersonalKnox/Reactions'
reactions.poll()
''')
print('Native B42 settings checks passed: defaults, persistence, command visibility, stale diagnostic action, no speech scans.')
