"""Use the installed B42 ModOptions implementation to check persistence and diagnostic gating."""
from pathlib import Path
import json
import re
from lupa.lua51 import LuaRuntime

root = Path(__file__).resolve().parents[1]
translations = root / 'common/media/lua/shared/Translate'
en = json.loads((translations / 'EN/UI.json').read_text())
ru = json.loads((translations / 'RU/UI.json').read_text())
own_key = lambda key: key.startswith(('UI_PersonalKnox_', 'UI_optionscreen_binding_PersonalKnox'))
en_keys, ru_keys = {key for key in en if own_key(key)}, {key for key in ru if own_key(key)}
assert en_keys == ru_keys, f'PersonalKnox UI translation keys differ: {en_keys ^ ru_keys}'
labels = set(re.findall(r'"(UI_PersonalKnox_[^"]+)"',
    (root / 'common/media/lua/client/PersonalKnox/Settings.lua').read_text()))
assert labels <= ru_keys, f'Missing settings labels/tooltips/choices: {labels - ru_keys}'
assert all(re.search('[А-Яа-яЁё]', ru[key]) for key in labels), 'Settings require Russian text'
for key in en_keys:
    assert sorted(re.findall(r'%\d+', en[key])) == sorted(re.findall(r'%\d+', ru[key])), key
native = Path('/home/j0schi/SteamLibrary/steamapps/common/ProjectZomboid/projectzomboid/media/lua/client/PZAPI/ModOptions.lua')
lua = LuaRuntime(unpack_returned_tuples=True)
lua.globals().package.path = str(root / 'common/media/lua/client/?.lua')
lua.globals().nativeOptionsSource = native.read_text()
lua.execute('''
function getText(key) return key end
package.preload['PZAPI/ModOptions']=function() assert(loadstring(nativeOptionsSource))();return PZAPI.ModOptions end
local settings=require 'PersonalKnox/Settings'
local options=PZAPI.ModOptions:getOptions('PersonalKnox')
assert(#PZAPI.ModOptions.Data==1 and #options.data==20,'Twenty PersonalKnox options')
assert(settings.speechEnabled() and not settings.diagnosticsEnabled(),'Speech on, diagnostics off by default')
assert(settings.autoMedic() and settings.autoDrink() and settings.autoEat(),'Self-care on by default')
assert(settings.infiniteDurability() and settings.infiniteAmmo() and settings.autoUnstick() and settings.careSearchBags() and settings.helpFrequency()==3 and settings.npcVoices() and settings.npcNeedRate()==3 and settings.chatterFrequency()==3 and settings.performanceMode()==false and settings.npcItemAsks()==true and settings.reloadMode()==2 and settings.ammoMode()==2 and settings.npcSearchContainers()==true and settings.npcBandageStock()==true and settings.npcStock()==true and settings.npcFinds()==true,'Upkeep, unstick, bag search, voices, need rate and help frequency defaults')
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
print(f'Russian UI translations passed: {len(labels)} settings labels, tooltips and choices; matching keys/placeholders.')
