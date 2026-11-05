-- Wounded companions bandage themselves with a clean bandage from their inventory.
local NpcCare = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local INTERVAL_MS, MAX_NPCS = 10000, 2
local lastCheck, deps = 0, nil

local function cleanBandage(body)
    local items = body:getInventory():getItems()
    local dirty = nil
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        if item:isCanBandage() then
            local kind = (item:getType() or ""):lower()
            if not kind:find("dirty", 1, true) then return item end
            dirty = dirty or item
        end
    end
    return dirty
end

local function bleedingPart(body)
    local parts = body:getBodyDamage():getBodyParts()
    for i = 0, parts:size() - 1 do
        local part = parts:get(i)
        if part:bleeding() and not part:bandaged() then return part end
    end
    return nil
end

local function poll(player)
    if isClient() or not deps or Perf.skip() or not Settings.autoMedic() then return end
    local now = getTimestampMs()
    if now - lastCheck < INTERVAL_MS then return end
    lastCheck = now
    local checked = 0
    for _, npc in ipairs(deps.roster(player)) do
        if checked >= MAX_NPCS then break end
        local body = npc:getWorldObject()
        if body and not body:isDead() then
            checked = checked + 1
            local part = bleedingPart(body)
            local bandage = part and cleanBandage(body)
            if part and bandage and ISApplyBandage and ISTimedActionQueue then
                ISTimedActionQueue.add(ISApplyBandage:new(body, body, bandage, part, true))
                if Settings.diagnosticsEnabled() then
                    print("[PersonalKnox][npc-care] bandage npc=" .. tostring(body))
                end
            end
        end
    end
end

function NpcCare.install(adapters)
    deps = adapters
    Events.OnGameStart.Add(function() lastCheck = 0 end)
    Events.OnPlayerUpdate.Add(function()
        local player = getSpecificPlayer(0)
        if not player or player:isDead() then return end
        local ok, err = pcall(poll, player)
        if not ok then print("[PersonalKnox] NPC-care error: " .. tostring(err)) end
    end)
end

return NpcCare
