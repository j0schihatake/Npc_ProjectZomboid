"""Offline check: Settings.lua applies values from the SQLite dump."""
from lupa.lua51 import LuaRuntime
from pathlib import Path

lua = LuaRuntime()
lua.execute(r'''
function getText(k) return k end
Keyboard = {KEY_INSERT=1, KEY_DELETE=2}
Events = {OnGameStart={Add=function() end}}
local options = {}
local page = {}
function page:getOption(k) return options[k] end
function page:addTickBox(k,l,d) local o={value=d}; function o:getValue() return self.value end; function o:setValue(v) self.value=v end; options[k]=o end
function page:addSlider(k,l,a,b,c,d) local o={value=d}; function o:getValue() return self.value end; function o:setValue(v) self.value=v end; options[k]=o end
function page:addComboBox(k,l,h) local o={value=1}; function o:addItem() end; function o:getValue() return self.value end; function o:setValue(v) self.value=v end; options[k]=o; return o end
function page:addKeyBind(k,l,d) local o={value=d}; function o:getValue() return self.value end; function o:setValue(v) self.value=v end; options[k]=o; return o end
PZAPI = {ModOptions={getOptions=function() return page end,create=function() return page end}}
package.preload['PZAPI/ModOptions'] = function() return PZAPI.ModOptions end
Saved = {}
PersonalKnoxAPI = {getInstance=function() return {
  dbSettings=function() return "performanceMode\ttrue\nzombieAttackOwnNpc\tfalse\n" end,
  dbSetSetting=function(k,v) Saved[k]=v end,
} end}
function TEST_PAGE() return page end
''')
path = Path(__file__).resolve().parents[1] / 'common/media/lua/client/PersonalKnox/Settings.lua'
chunk = lua.eval('loadstring')(path.read_text(), str(path))
settings = chunk()
assert settings is not None
assert settings.performanceMode() is True
assert settings.zombieAttackOwnNpc() is False
lua.execute('TEST_PAGE():apply()')
assert lua.eval('Saved.performanceMode') == 'true'
print('Settings DB tests passed: SQLite values override option defaults.')
