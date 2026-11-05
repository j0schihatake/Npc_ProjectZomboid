"""Exercise world-reaction adapters without a game or a scan of the whole zombie list."""
from pathlib import Path
from lupa.lua51 import LuaRuntime

root=Path(__file__).resolve().parents[1]
lua=LuaRuntime(unpack_returned_tuples=True)
lua.globals().package.path=str(root/'common/media/lua/shared/?.lua')+';'+str(root/'common/media/lua/client/?.lua')
lua.execute('''
function getText(key, ...) return key end
local tick,reset
Events={EveryOneMinute={Add=function(fn) tick=fn end},OnGameStart={Add=function(fn) reset=fn end}}
local emitted,observed,resets=0,{},0
local config={}
local damage={getHealth=function() return 100 end,getNumPartsBleeding=function() return 0 end}
local square={isOutside=function() return true end,getDeadBody=function() return nil end}
local player={getModData=function() return config end,isDead=function() return false end,
    getX=function() return 0 end,getY=function() return 0 end,getZ=function() return 0 end,
    getBodyDamage=function() return damage end}
local visible=true
local visionCalls=0
local bodies={}
for i=1,8 do
    local body={kind="IsoNpcPlayer",getX=player.getX,getY=player.getY,getZ=player.getZ,
        isDead=player.isDead,getCurrentSquare=function() return square end,
        getBodyDamage=function() return damage end,
        CanSee=function() visionCalls=visionCalls+1; return visible end}
    local meta={id=i,getId=function(self) return self.id end,getWorldObject=function() return body end}
    body.getMetaCharacter=function() return meta end
    bodies[#bodies+1]=body
end
for i=1,500 do bodies[#bodies+1]={kind="IsoZombie",isDead=player.isDead,getX=player.getX,getY=player.getY} end
local objects={size=function() return #bodies end,get=function(_,i) return bodies[i+1] end}
square.getMovingObjects=function() return objects end
local queries=0
function getCell() return {
    getGridSquare=function(_,x,y,z) queries=queries+1; assert(queries<=169); return square end,
    getZombieList=function() error("World reactions must not traverse the whole zombie list") end
} end
function instanceof(obj,kind) return obj.kind==kind end
function getSpecificPlayer() return player end
local client=false
function isClient() return client end
function getTimestampMs() return 100000 end
function getGameTime() return {getTimeOfDay=function() return 22 end} end
function getClimateManager() return {getRainIntensity=function() return 1 end} end
classFilesInstalled=true
local java={
    worldReactionTopic=function(_,key,now,hp,php,bleed,zombies,rain,dark,corpses)
        observed[#observed+1]={key=key,zombies=zombies,rain=rain,dark=dark}
        assert(zombies<=24 and now==100000 and hp==100)
        return "react_zombies"
    end,
    resetWorldReactions=function() resets=resets+1 end
}
PersonalKnoxAPI={getInstance=function() return java end}
local reactions=require "PersonalKnox/Reactions"
reactions.install({isMember=function() return true end,say=function() emitted=emitted+1 end})
reset()
assert(resets==1)
tick()
assert(#observed==4 and emitted==4,"One poll handles at most four companions")
assert(visionCalls<=96,"Visibility work has a hard budget")
assert(observed[1].zombies==24 and observed[1].rain and observed[1].dark)
local first=observed[1].key
observed={}; queries=0; visible=false;visionCalls=0
tick()
assert(#observed==4 and observed[1].key~=first,"Companions rotate")
assert(observed[1].zombies==0,"Hidden zombies must not trigger danger")
config.PersonalKnox.worldReactions=false
observed={}; queries=0
tick()
assert(#observed==0 and queries==0,"Disabled reactions do no world work")
config.PersonalKnox.worldReactions=true
client=true
tick()
assert(#observed==0 and queries==0,"Unsupported multiplayer must not run this SP adapter")
''')
print('Reaction Lua checks passed: bounded local sampling, rotating NPC budget, visibility, disable switch, SP guard.')
