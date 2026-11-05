"""Verify optional target-specific UI bridges and callback identity."""
from pathlib import Path
from lupa.lua51 import LuaRuntime

root=Path(__file__).resolve().parents[1]
lua=LuaRuntime(unpack_returned_tuples=True)
lua.globals().package.path=str(root/'common/media/lua/client/?.lua')
viewpoint = Path('/home/j0schi/SteamLibrary/steamapps/workshop/content/108600/3809306528/mods/Viewpoint/42/media/lua/client/Viewpoint_Interact.lua')
lua.globals().viewpointSource = viewpoint.read_text()
knox = Path('/home/j0schi/SteamLibrary/steamapps/workshop/content/108600/3397396843/mods/KnoxEventExpanded/42/media/lua/client/00_UI/NpcContextualActionListUI.lua')
lua.globals().knoxUiSource = knox.read_text()
lua.execute('''
function getText(key) return key end
function isClient() return false end
classFilesInstalled=true;PersonalKnoxAPI={}
Events={OnGameStart={Add=function(fn) attach=fn end}}
local member=true
local body={getX=function() return 1 end,getY=function() return 0 end,getZ=function() return 0 end,isDead=function() return false end}
local npc={getWorldObject=function() return body end}
body.getMetaCharacter=function() return npc end
local stranger={getWorldObject=function() return body end}
local player={getX=function() return 0 end,getY=function() return 0 end,getZ=function() return 0 end,
 isDead=function() return false end,CanSee=function() return true end,getPlayerNum=function() return 0 end}
function getSpecificPlayer() return player end
function instanceof(object,kind) return object==body and kind=='IsoNpcPlayer' end
function getPlayerScreenLeft() return 0 end
function getPlayerScreenTop() return 0 end
function getPlayerScreenWidth() return 1000 end
function getPlayerScreenHeight() return 800 end
local opened=0
ISContextMenu={get=function(num,x,y) assert(num==0 and x==500 and y==400);opened=opened+1;return {} end}
NpcContextualActionListUI={buildNpcActions=function() return {{label='native'}} end}
ViewpointInteract={harvest=function(_,object)
 ViewpointInteract.actions={{name='native',fn=function() end,args={},n=0,enabled=true}}
 return {labels={'native'},enabled={true},title='target'}
end}
local Interaction=require 'PersonalKnox/Interaction'
Interaction.install({isMember=function(_,target) return member and target==npc end,
 reactions=function() end,fill=function(_,p,target) assert(p==player and target==npc) end})
attach();attach()
local panel={playerNum=0}
local rows=NpcContextualActionListUI.buildNpcActions(panel,npc)
assert(#rows==2 and rows[2].label=='native',"Preserve native actions and attach only once")
assert(#NpcContextualActionListUI.buildNpcActions(panel,stranger)==1)
rows[1].onSelect();assert(opened==1)
local result=ViewpointInteract.harvest(player,body)
assert(#result.labels==2 and #result.enabled==2 and #ViewpointInteract.actions==2)
local action=ViewpointInteract.actions[2]
assert(action.args[2]==npc and action.n==2)
action.fn(unpack(action.args,1,action.n));assert(opened==2)
member=false
action.fn(unpack(action.args,1,action.n));assert(opened==2,"Recheck on activation")
assert(#ViewpointInteract.harvest(player,body).labels==1)
member=true;player.CanSee=function() return false end
assert(#ViewpointInteract.harvest(player,body).labels==1,"No interaction through walls")
assert(#ViewpointInteract.harvest(player,{}).labels==1,"No squad actions on furniture")
-- Run the installed Viewpoint collector and dispatcher too, not only doubles.
local context={numOptions=1,options={}}
ISWorldObjectContextMenu={createMenu=function() return context end}
HaloTextHelper={}
function getTexture() return nil end
function isoToScreenX() return 0 end
function isoToScreenY() return 0 end
function getPlayerContextMenu() return {getIsVisible=function() return false end,hideAndChildren=function() end} end
function getCell() return {getDrag=function() return nil end} end
body.getSquare=function() return {getX=body.getX,getY=body.getY,getZ=body.getZ} end
player.CanSee=function() return true end
assert(loadstring(viewpointSource))()
Interaction.viewpointAttached=false
Interaction.attach()
local actual=ViewpointInteract.harvest(player,body)
assert(#actual.labels==1 and actual.enabled[1] and #ViewpointInteract.actions==1)
ViewpointInteract.run(player,1)
assert(opened==3,"Installed Viewpoint dispatcher must open our selected NPC menu")
package.preload.KnoxEventLoader=function() return {} end
package.preload['ISUI/ISPanelJoypad']=function() return {} end
ISPanelJoypad={derive=function() return {} end}
UIFont={Small=1,Medium=2}
function getTextManager() return {getFontHeight=function() return 20 end} end
function getDebug() return false end
function isNpcInGroup(_,target) return target==npc end
assert(loadstring(knoxUiSource))()
Interaction.knoxAttached=false
Interaction.attach()
local nativeRows=NpcContextualActionListUI.buildNpcActions(panel,npc)
assert(#nativeRows==5 and nativeRows[1].label=='ContextMenu_PersonalKnox_Squad')
local ended=false
panel.endSession=function() ended=true end
NpcContextualActionListUI.onRowClick(panel,{contextualAction=nativeRows[1],rowIndex=1})
assert(ended and opened==4,"Installed Knox click must close its session and open our menu")
''')
print('Interaction checks passed: native preservation, single attachment, exact target, list alignment, selection revalidation, visibility, installed Viewpoint collector/dispatcher and Knox menu/click.')
