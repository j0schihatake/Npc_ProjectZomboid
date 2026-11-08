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
        local api = PersonalKnoxAPI.getInstance()
        local px, py, pz = player:getX(), player:getY(), player:getZ()
        local members = Settings.factionMembers()
        local supplies = Settings.factionSupplies()
        local garrison = Settings.outpostGarrison()
        local outpostSupplies = Settings.outpostSupplies()
        if data.PersonalKnoxFactions and data.PersonalKnoxFactionsData then
            local restored = 0
            for line in tostring(data.PersonalKnoxFactionsData):gmatch("[^\n]+") do
                local id, faction, x, y, z, savedMembers, savedSupplies, savedGarrison, savedOutpostSupplies =
                    line:match("^([^|]+)|([^|]+)|([^|]+)|([^|]+)|([^|]+)|([^|]+)|([^|]+)|([^|]+)|([^|]+)$")
                if not id then
                    id, faction, x, y, z = line:match("^([^|]+)|([^|]+)|([^|]+)|([^|]+)|([^|]+)$")
                end
                if id and faction then
                    api:alifeSquad(id, faction, tonumber(savedMembers) or members, 100, tonumber(savedSupplies) or supplies,
                        "PATROL", tonumber(x), tonumber(y), tonumber(z), tonumber(x), tonumber(y))
                    api:ownGroup(id, faction, tonumber(savedMembers) or members, tonumber(x), tonumber(y))
                    api:ownGroupMission(id, tonumber(x) + 18, tonumber(y) + 12, tonumber(z))
                    api:alifeOutpost("outpost-" .. id, faction, tonumber(x), tonumber(y), tonumber(z),
                        tonumber(savedGarrison) or garrison, tonumber(savedOutpostSupplies) or outpostSupplies, false)
                    restored = restored + 1
                end
            end
            print("[PersonalKnox][factions] restored=" .. tostring(restored))
            return
        end
        local distance = Settings.factionDistance()
        local rows = {}
        for i = 1, count do
            local x = px + ZombRand(distance * 2) - distance
            local y = py + ZombRand(distance * 2) - distance
            local faction = NAMES[((i - 1) % #NAMES) + 1]
            local id = "own-faction-" .. i
            api:alifeSquad(id, faction, members, 100, supplies, "PATROL", x, y, pz, x, y)
            api:ownGroup(id, faction, members, x, y)
            api:ownGroupMission(id, x + 18, y + 12, pz)
            api:alifeOutpost("own-outpost-" .. i, faction, x, y, pz, garrison, outpostSupplies, false)
            table.insert(rows, table.concat({id, faction, tostring(x), tostring(y), tostring(pz),
                tostring(members), tostring(supplies), tostring(garrison), tostring(outpostSupplies)}, "|"))
        end
        data.PersonalKnoxFactionsData = table.concat(rows, "\n")
        data.PersonalKnoxFactions = true
        print("[PersonalKnox][factions] count=" .. tostring(count) .. " distance=" .. tostring(distance))
    end)
end

return OwnFactions
