-- Infinite-durability mode for companions: keep their held weapons at full condition.
local NpcUpkeep = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local INTERVAL_MS, MAX_NPCS = 5000, 2
local lastCheck, deps = 0, nil

local function repair(body)
    for _, item in ipairs({ body:getPrimaryHandItem(), body:getSecondaryHandItem() }) do
        if item and item.getCondition and item.getConditionMax and item.setCondition then
            local max = item:getConditionMax()
            if max > 0 and item:getCondition() < max then
                item:setCondition(max)
                if Settings.diagnosticsEnabled() then
                    print("[PersonalKnox][npc-upkeep] repair=" .. tostring(item:getFullType()))
                end
            end
        end
    end
end

local function poll(player)
    if isClient() or not deps or Perf.skip() or not Settings.infiniteDurability() then return end
    local now = getTimestampMs()
    if now - lastCheck < INTERVAL_MS then return end
    lastCheck = now
    local checked = 0
    for _, npc in ipairs(deps.roster(player)) do
        if checked >= MAX_NPCS then break end
        local body = npc:getWorldObject()
        if body and not body:isDead() then
            checked = checked + 1
            repair(body)
        end
    end
end

function NpcUpkeep.install(adapters)
    deps = adapters
    Events.OnGameStart.Add(function() lastCheck = 0 end)
    Events.OnPlayerUpdate.Add(function()
        local player = getSpecificPlayer(0)
        if not player or player:isDead() then return end
        local ok, err = pcall(poll, player)
        if not ok then print("[PersonalKnox] NPC-upkeep error: " .. tostring(err)) end
    end)
end

return NpcUpkeep
