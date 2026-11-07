-- Random strategic groups, factions and NPC points for the own A-Life.
local OwnFactions = {}
local Settings = require "PersonalKnox/Settings"

local NAMES = {"Survivors", "Raiders", "Military", "Traders"}

function OwnFactions.install()
    Events.OnGameStart.Add(function()
        if not PersonalKnoxAPI then return end
        local count = Settings.factionCount()
        if count <= 0 then return end
        local player = getSpecificPlayer(0)
        if not player then return end
        local data = player:getModData()
        if data.PersonalKnoxFactions then return end
        local api = PersonalKnoxAPI.getInstance()
        local px, py, pz = player:getX(), player:getY(), player:getZ()
        for i = 1, count do
            local x = px + ZombRand(160) - 80
            local y = py + ZombRand(160) - 80
            local faction = NAMES[((i - 1) % #NAMES) + 1]
            api:alifeSquad("own-faction-" .. i, faction, 3, 100, 2, "PATROL", x, y, pz, x, y)
            api:alifeOutpost("own-outpost-" .. i, faction, x, y, pz, 1, 1, false)
        end
        data.PersonalKnoxFactions = true
        print("[PersonalKnox][factions] count=" .. tostring(count))
    end)
end

return OwnFactions
