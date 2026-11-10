"""Check OwnNpcMedic against mocked own-layer bodies."""
from pathlib import Path
from lupa.lua51 import LuaRuntime

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / 'common/media/lua/client/PersonalKnox/OwnNpcMedic.lua'
PRELUDE = r'''
package.loaded['PersonalKnox/Settings'] = {autoMedic=function() return true end,
    diagnosticsEnabled=function() return false end}
package.loaded['PersonalKnox/Perf'] = {skip=function() return false end}
function isClient() return false end
BodyPartType={LowerLeg_L='LowerLeg_L',LowerLeg_R='LowerLeg_R',
    UpperLeg_L='UpperLeg_L',UpperLeg_R='UpperLeg_R'}
queue={}
ISTimedActionQueue={add=function(action)table.insert(queue,action)end}
ISSplint={new=function(self,medic,patient,sheet,plank,part,doIt)
    local action={kind='splint',medic=medic,patient=patient,sheet=sheet,plank=plank,part=part}
    return action
end}
ISApplyBandage={new=function(self,medic,patient,item,part,doIt)
    local action={kind='bandage',medic=medic,patient=patient,item=item,part=part}
    return action
end}
function arrays(list)
    return {items=list,size=function(self)return #self.items end,
        get=function(self,i)return self.items[i+1]end}
end
function item(fullType,canBandage)
    local kind=fullType:match('([^%.]+)$')
    return {getFullType=function()return fullType end,getType=function()return kind end,
        isCanBandage=function()return canBandage or false end}
end
function inventory(items)return {getItems=function()return arrays(items)end}end
function legPart(fractured,bleeding)
    return {getType=function()return BodyPartType.LowerLeg_L end,
        getFractureTime=function()return fractured and 30 or 0 end,
        getSplintFactor=function()return 0 end,
        bleeding=function()return bleeding or false end,
        bandaged=function()return false end}
end
function body(parts,items)
    return {isDead=function()return false end,hasTimedActions=function()return false end,
        getX=function()return 0 end,getY=function()return 0 end,
        getBodyDamage=function()return {getBodyParts=function()return arrays(parts)end}end,
        getInventory=function()return inventory(items)end}
end
patientBody=body({legPart(true,false)},{})
medicItems={}
medicBody=body({},medicItems)
bodies={p=patientBody,m=medicBody}
api={ownCount=function()return 2 end,
    ownId=function(_,i)return i==0 and 'p' or 'm' end,
    ownBodyObject=function(_,id)return bodies[id] end}
PersonalKnoxAPI={getInstance=function()return api end}
'''
def run(kind):
    lua=LuaRuntime(unpack_returned_tuples=True)
    lua.execute(PRELUDE)
    if kind == 'ready':
        lua.execute("table.insert(medicItems,item('Base.Splint',false))")
    elif kind == 'field':
        lua.execute("table.insert(medicItems,item('Base.RippedSheets',false));"
                    "table.insert(medicItems,item('Base.Plank',false))")
    elif kind == 'bandage':
        lua.execute("patientBody=body({legPart(false,true)},{fracturedPart=nil});"
                    "bodies.p=patientBody;table.insert(medicItems,item('Base.Bandage',true))")
    module=lua.execute(SCRIPT.read_text())
    return module.tick(), lua.globals().queue

for kind, expected in [('ready','splint'),('field','splint'),('bandage','bandage')]:
    result, queue = run(kind)
    assert result == expected, (kind, result)
    assert len(queue) == 1, (kind, len(queue))
print('Own-NPC medic tests passed: ready splint, field splint, bandage.')
