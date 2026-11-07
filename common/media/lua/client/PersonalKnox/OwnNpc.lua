-- Thin game-event bridge; own NPC state and bodies live in the Java core.
local OwnNpc = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local Util = require "PersonalKnox/Util"

local deps
local INTERVAL_MS, LOG_MS = 2000, 60000
local last, lastLog, logged, lastSave = 0, 0, false, 0

local function saveState()
    if isClient() or not PersonalKnoxAPI then return end
    local player=getSpecificPlayer(0)
    if not player then return end
    local api=PersonalKnoxAPI.getInstance()
    local data=player:getModData()
    local agents,population,targetZ=api:ownSnapshot(),api:ownPopulationSnapshot(),api:ownTargetZSnapshot()
    data.PersonalKnoxOwnNpc=agents
    data.PersonalKnoxOwnPopulation=population
    data.PersonalKnoxTargetZ=targetZ
end

local function tick()
    if isClient() or not PersonalKnoxAPI then return end
    local player = getSpecificPlayer(0)
    if not player or player:isDead() then return end
    local now = getTimestampMs()
    local interval = INTERVAL_MS
    local okFps, fps = pcall(function() return getAverageFPS() end)
    if okFps and type(fps) == "number" and fps > 0 and fps < 30 then interval = 5000 end
    if now - last < interval then return end
    last = now
    if Perf.skip() then return end
    local api = PersonalKnoxAPI.getInstance()
    local members = api:knoxGroupMembers(player)
    if members == 0 then
        for _, npc in ipairs(deps.roster(player)) do
            if npc:getWorldObject() and not npc:getWorldObject():isDead() then members = members + 1 end
        end
    end
    local px, py, pz = player:getX(), player:getY(), player:getZ()
    api:ownGroup("player-squad", "own", members, px, py)
    api:ownSync(px, py, 80, members)
    api:ownFollowTarget3(px, py, pz)
    api:ownTick(0.01, 32)
    local active = api:ownActive()
    local bodies = api:ownBodiesSync()
    if now - lastSave >= 60000 then
        lastSave = now
        saveState()
    end
    if not logged then
        logged = true
        print("[PersonalKnox][knox-roster] " .. tostring(api:knoxMemberSnapshot(player)))
        print("[PersonalKnox][own-npc] members=" .. tostring(members) .. " active=" .. tostring(active) .. " bodies=" .. tostring(bodies) .. " " .. tostring(api:knoxStatus()))
    elseif Settings.diagnosticsEnabled() and now - lastLog >= LOG_MS then
        lastLog = now
        print("[PersonalKnox][own-npc] members=" .. tostring(members) .. " active=" .. tostring(active) .. " bodies=" .. tostring(bodies) .. " " .. tostring(api:knoxStatus()))
    end
end

function OwnNpc.install(adapters)
    deps = adapters
    Events.OnGameStart.Add(function()
        local player = getSpecificPlayer(0)
        local data = player and player:getModData()
        if data and data.PersonalKnoxOwnNpc and PersonalKnoxAPI then
            local ok, err = pcall(function() PersonalKnoxAPI.getInstance():ownRestore(data.PersonalKnoxOwnNpc) end)
            if not ok then print("[PersonalKnox] Own-NPC restore error: " .. tostring(err)) end
        end
        if data and data.PersonalKnoxTargetZ and PersonalKnoxAPI then
            pcall(function() PersonalKnoxAPI.getInstance():ownTargetZRestore(data.PersonalKnoxTargetZ) end)
        end
        if data and data.PersonalKnoxOwnPopulation and PersonalKnoxAPI then
            local ok, err = pcall(function() PersonalKnoxAPI.getInstance():ownPopulationRestore(data.PersonalKnoxOwnPopulation) end)
            if not ok then print("[PersonalKnox] Own population restore error: " .. tostring(err)) end
        end
        local count = 0
        for _, n in ipairs({1, 2, 3, 4}) do
            if player:hasTrait("PersonalKnox:Companions" .. tostring(n)) then count = n end
        end
        if count == 0 then count = Settings.companionCount() end
        if count > 0 and not (data and data.PersonalKnoxOwnNpc) and PersonalKnoxAPI then
            local api = PersonalKnoxAPI.getInstance()
            for i = 1, count do
                local ok, created = pcall(function() return api:ownCreateCompanion(player) end)
                if ok then
                    print("[PersonalKnox][own-npc] starting companion " .. tostring(created))
                else
                    print("[PersonalKnox] Starting companion error: " .. tostring(created))
                end
            end
        end
    end)
    Events.OnSave.Add(function()
        local ok,err=pcall(saveState)
        if not ok then print("[PersonalKnox] Own-NPC save error: "..tostring(err)) end
    end)
    Events.OnPlayerUpdate.Add(function()
        local ok, err = pcall(tick)
        if not ok then print("[PersonalKnox] Own-NPC poll error: " .. tostring(err)) end
    end)
end

return OwnNpc
