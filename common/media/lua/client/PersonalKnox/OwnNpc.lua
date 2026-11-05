-- Distance sync for our own NPC layer; bodies are created by the future adapter.
local OwnNpc = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local Util = require "PersonalKnox/Util"

local deps
local INTERVAL_MS, LOG_MS = 10000, 60000
local last, lastLog, logged, lastSave = 0, 0, false, 0

local function tick()
    if isClient() or not PersonalKnoxAPI then return end
    local player = getSpecificPlayer(0)
    if not player or player:isDead() then return end
    local now = getTimestampMs()
    if now - last < INTERVAL_MS then return end
    last = now
    if Perf.skip() then return end
    local api = PersonalKnoxAPI.getInstance()
    local members = 0
    for _, npc in ipairs(deps.roster(player)) do
        if npc:getWorldObject() and not npc:getWorldObject():isDead() then members = members + 1 end
    end
    local px, py = player:getX(), player:getY()
    api:ownGroup("player-squad", "own", members, px, py)
    local active = api:ownSync(px, py, 80, members)
    if now - lastSave >= 60000 then
        lastSave = now
        player:getModData().PersonalKnoxOwnNpc = api:ownSnapshot()
    end
    if not logged then
        logged = true
        print("[PersonalKnox][own-npc] members=" .. tostring(members) .. " active=" .. tostring(active))
    elseif Settings.diagnosticsEnabled() and now - lastLog >= LOG_MS then
        lastLog = now
        print("[PersonalKnox][own-npc] members=" .. tostring(members) .. " active=" .. tostring(active))
    end
end

function OwnNpc.install(adapters)
    deps = adapters
    Events.OnGameStart.Add(function()
        local player = getSpecificPlayer(0)
        local saved = player and player:getModData().PersonalKnoxOwnNpc
        if saved and PersonalKnoxAPI then
            local ok, err = pcall(function() PersonalKnoxAPI.getInstance():ownRestore(saved) end)
            if not ok then print("[PersonalKnox] Own-NPC restore error: " .. tostring(err)) end
        end
    end)
    Events.OnPlayerUpdate.Add(function()
        local ok, err = pcall(tick)
        if not ok then print("[PersonalKnox] Own-NPC poll error: " .. tostring(err)) end
    end)
end

return OwnNpc
