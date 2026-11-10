-- Zombies should consider own NPC bodies as targets, not only the player.
local ZombieAggro = {}
local Settings = require "PersonalKnox/Settings"

function ZombieAggro.install()
    Events.OnZombieUpdate.Add(function(zombie)
        if not zombie or not Settings.zombieAttackOwnNpc() or not PersonalKnoxAPI then return end
        local player = getSpecificPlayer(0)
        if not player then return end
        local ok, target = pcall(function() return zombie:getTarget() end)
        if not ok then return end
        if target and target ~= player then return end
        pcall(function() PersonalKnoxAPI.getInstance():zombieRetarget(zombie) end)
    end)
end

return ZombieAggro
