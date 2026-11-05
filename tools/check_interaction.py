"""Exercise installed Viewpoint and Knox Lua: direct actions must never open a mouse-only command popup."""
from pathlib import Path
from lupa.lua51 import LuaRuntime

root = Path(__file__).resolve().parents[1]
lua = LuaRuntime(unpack_returned_tuples=True)
lua.globals().package.path = str(root/'common/media/lua/client/?.lua')
workshop = Path('/home/j0schi/SteamLibrary/steamapps/workshop/content/108600')
lua.globals().viewpointSource = (workshop/'3809306528/mods/Viewpoint/42/media/lua/client/Viewpoint_Interact.lua').read_text()
lua.globals().knoxUiSource = (workshop/'3397396843/mods/KnoxEventExpanded/42/media/lua/client/00_UI/NpcContextualActionListUI.lua').read_text()
lua.execute('''
function getText(key,a,b)
 if key=='ContextMenu_PersonalKnox_ActionLabel' then return a..': '..b end
 return key
end
function isClient() return false end
classFilesInstalled=true;PersonalKnoxAPI={}
Events={OnGameStart={Add=function(fn) attach=fn end}}
local member,visible=true,true
local body={kind='IsoNpcPlayer',getX=function() return 1 end,getY=function() return 0 end,
 getZ=function() return 0 end,isDead=function() return false end}
local npc={getWorldObject=function() return body end}
body.getMetaCharacter=function() return npc end
body.getSquare=function() return {getX=body.getX,getY=body.getY,getZ=body.getZ} end
local player={getX=function() return 0 end,getY=function() return 0 end,getZ=function() return 0 end,
 isDead=function() return false end,CanSee=function() return visible end,getPlayerNum=function() return 0 end}
function getSpecificPlayer() return player end
function instanceof(object,kind) return type(object)=='table' and object.kind==kind end
function getPlayerScreenLeft() return 0 end
function getPlayerScreenTop() return 0 end
function getPlayerScreenWidth() return 1000 end
function getPlayerScreenHeight() return 800 end
local opened=0
ISContextMenu={get=function() opened=opened+1;return {addOption=function() end} end}
local context={numOptions=1,options={}}
ISWorldObjectContextMenu={createMenu=function() return context end}
HaloTextHelper={}
function getTexture() return nil end
function isoToScreenX() return 0 end
function isoToScreenY() return 0 end
function getPlayerContextMenu() return {getIsVisible=function() return false end,hideAndChildren=function() end} end
function getCell() return {getDrag=function() return nil end} end
package.preload.KnoxEventLoader=function() return {} end
package.preload['ISUI/ISPanelJoypad']=function() return {} end
ISPanelJoypad={derive=function() return {} end}
UIFont={Small=1,Medium=2}
function getTextManager() return {getFontHeight=function() return 20 end} end
function getDebug() return false end
function isNpcInGroup(_,target) return member and target==npc end
local panel={playerNum=0}
function getPlayerData() return {npcContextualActionListUI=panel} end
local spoken,medical,inventory=0,0,0
function talkToNpc(_,target,num) assert(target==npc and num==0);spoken=spoken+1 end
function requestNpcMedicalCheck(num,target) assert(target==npc and num==0);medical=medical+1 end
function requestNpcInventory(num,target) assert(target==npc and num==0);inventory=inventory+1 end
assert(loadstring(viewpointSource))()
assert(loadstring(knoxUiSource))()
local Interaction=require 'PersonalKnox/Interaction'
local tasks,reactions=0,true
Interaction.install({isMember=function(_,target) return member and target==npc end,
 reactions=function(menu,p)
  menu:addOption(reactions and 'Reactions off' or 'Reactions on',p,function() reactions=not reactions end)
 end,
 fill=function(menu,p,target)
  assert(p==player and target==npc)
  menu:addOption('Task',p,function(actor,receiver,empty,topic)
   assert(actor==player and receiver==npc and empty==nil and topic=='status')
   tasks=tasks+1
  end,target,nil,'status')
 end})
attach();attach()
local nativeRows=NpcContextualActionListUI.buildNpcActions(panel,npc)
assert(#nativeRows==5 and nativeRows[1].label=='ContextMenu_PersonalKnox_Squad',"Attach only once and preserve native UI")
local result=ViewpointInteract.harvest(player,body)
assert(#result.labels==5 and #result.enabled==5 and #ViewpointInteract.actions==5)
local invAction,taskAction,reactionAction
for i,label in ipairs(result.labels) do
 assert(label==ViewpointInteract.actions[i].name and result.enabled[i])
 if label=='Knox: ContextMenu_KnoxEvent_OpenInventory' then invAction=ViewpointInteract.actions[i]
 elseif label=='ContextMenu_PersonalKnox_Squad: Task' then taskAction=ViewpointInteract.actions[i]
 elseif label=='ContextMenu_PersonalKnox_Squad: Reactions off' then reactionAction=ViewpointInteract.actions[i]
 else ViewpointInteract.run(player,i) end
end
assert(spoken==1 and medical==1)
local function invoke(action) action.fn(unpack(action.args,1,action.n)) end
invoke(invAction);assert(inventory==1)
invoke(taskAction);assert(tasks==1,"Preserve nil arguments and selected NPC identity")
invoke(reactionAction);assert(not reactions)
invoke(reactionAction);assert(not reactions,"Stale toggle must not perform a different current action")
assert(opened==0,"FPS actions must stay in Viewpoint; no mouse-only command popup")
member=false
invoke(invAction);invoke(taskAction)
assert(inventory==1 and tasks==1,"Rebuild authorization on selection")
local outsider=ViewpointInteract.harvest(player,body)
assert(#outsider.labels==2,"Strangers get native talk/medical only")
visible=false
assert(#ViewpointInteract.harvest(player,body).labels==0,"No through-wall interactions")
member=true;visible=true
body.getX=function() return 11 end
assert(#ViewpointInteract.harvest(player,body).labels==0,"Distance must be rechecked")
body.getX=function() return 1 end
local furniture={getSquare=body.getSquare}
assert(#ViewpointInteract.harvest(player,furniture).labels==0,"Leave unrelated object actions alone")
-- Ordinary Knox UI still uses its own session lifecycle.
local ended=false
panel.endSession=function() ended=true end
NpcContextualActionListUI.onRowClick(panel,{contextualAction=nativeRows[1],rowIndex=1})
assert(ended and opened==1)
''')
print('Interaction checks passed with installed Viewpoint/Knox: direct dispatch without command popup, target/argument identity, native actions, current permissions, stale toggles, visibility, distance, single attachment.')
