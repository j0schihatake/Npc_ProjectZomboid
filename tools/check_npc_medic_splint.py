"""Check the real NpcMedic Lua adapter against mocked game bodies."""
from pathlib import Path
from lupa.lua51 import LuaRuntime

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / 'common/media/lua/client/PersonalKnox/NpcMedic.lua'

PRELUDE = r'''
package.loaded['PersonalKnox/Settings'] = {autoMedic=function() return true end,
    diagnosticsEnabled=function() return false end}
package.loaded['PersonalKnox/Perf'] = {skip=function() return false end}
now=20000
function isClient() return false end
function getTimestampMs() return now end
function getSpecificPlayer() return player end
player={isDead=function() return false end}
events={start={},update={}}
Events={OnGameStart={Add=function(f)table.insert(events.start,f)end},
        OnPlayerUpdate={Add=function(f)table.insert(events.update,f)end}}
BodyPartType={LowerLeg_L='LowerLeg_L',LowerLeg_R='LowerLeg_R',
    UpperLeg_L='UpperLeg_L',UpperLeg_R='UpperLeg_R'}
queue={}
ISTimedActionQueue={add=function(action)table.insert(queue,action)end}
ISSplint={new=function(self,character,other,sheet,plank,part,doIt)
    local action={kind='splint',character=character,other=other,sheet=sheet,
        plank=plank,part=part,doIt=doIt}
    return action
end}
function arrays(list)
    return {items=list,size=function(self)return #self.items end,
        get=function(self,i)return self.items[i+1]end}
end
function item(fullType)return {getFullType=function()return fullType end}end
function inventory(items)return {getItems=function()return arrays(items)end}end
function fracturedPart()
    return {getType=function()return BodyPartType.LowerLeg_L end,
        getFractureTime=function()return 30 end,
        getSplintFactor=function()return 0 end,
        bleeding=function()return false end,
        bandaged=function()return false end}
end
function bodyWithParts(parts,items)
    return {isDead=function()return false end,hasTimedActions=function()return false end,
        getX=function()return 0 end,getY=function()return 0 end,
        getBodyDamage=function()return {getBodyParts=function()return arrays(parts)end}end,
        getInventory=function()return inventory(items)end}
end
patientBody=bodyWithParts({fracturedPart()},{})
splintItems={}
medicBody=bodyWithParts({},splintItems)
patient={getWorldObject=function()return patientBody end}
medic={getWorldObject=function()return medicBody end}
roster={patient,medic}
function fire(name)
    local copy={};for i,f in ipairs(events[name])do copy[i]=f end
    for _,f in ipairs(copy)do f() end
end
'''

def run(splint=False, sheet=False, plank=False):
    lua = LuaRuntime(unpack_returned_tuples=True)
    lua.execute(PRELUDE)
    items = []
    if splint:
        items.append("Base.Splint")
    if sheet:
        items.append("Base.RippedSheets")
    if plank:
        items.append("Base.Plank")
    lua.execute("for i=#splintItems,1,-1 do table.remove(splintItems,i) end;" + "".join(
        f"table.insert(splintItems,item('{value}'))" for value in items))
    module = lua.execute(SCRIPT.read_text())
    lua.execute("module=...;module.install({roster=function()return roster end});fire('start');fire('update')",
                module)
    return lua.globals().queue

q = run(splint=True)
assert len(q) == 1 and q[1]['kind'] == 'splint' and q[1]['plank']['getFullType']() == 'Base.Splint', 'ready splint'
q = run(sheet=True, plank=True)
assert len(q) == 1 and q[1]['sheet'] is not None and q[1]['plank'] is not None, 'field splint'
q = run()
assert len(q) == 0, 'no materials must not act'
print('NPC medic splint tests passed: ready Base.Splint, field sheet+plank, no-material skip.')
