-- Hungry/thirsty companions eat and drink from their own inventory.
local NpcEat = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local INTERVAL_MS, MAX_NPCS = 10000, 2
local lastCheck, deps = 0, nil

local function findFood(body)
    local items = body:getInventory():getItems()
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        if item:IsFood() then return item end
    end
    return nil
end

local function findWater(body)
    local items = body:getInventory():getItems()
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        local fluid = item:getFluidContainer()
        if fluid and fluid:getAmount() > 0 then
            local primary = fluid:getPrimaryFluid()
            if primary and primary:getFluidTypeString() == "Water" then return item end
        end
    end
    return nil
end

local function eat(body, item, drink)
    if drink and ISDrinkFluidAction and ISTimedActionQueue then
        ISTimedActionQueue.add(ISDrinkFluidAction:new(body, item, 1))
        return true
    elseif not drink and ISEatFoodAction and ISTimedActionQueue then
        ISTimedActionQueue.add(ISEatFoodAction:new(body, item, 1))
        return true
    end
    return false
end

local function poll(player)
    if isClient() or not deps or Perf.skip() then return end
    local now = getTimestampMs()
    if now - lastCheck < INTERVAL_MS then return end
    lastCheck = now
    local checked = 0
    for _, npc in ipairs(deps.roster(player)) do
        if checked >= MAX_NPCS then break end
        local body = npc:getWorldObject()
        if body and not body:isDead() and not body:hasTimedActions() then
            checked = checked + 1
            local hunger = body:getStats():get(CharacterStat.HUNGER)
            local thirst = body:getStats():get(CharacterStat.THIRST)
            if hunger >= 0.60 and Settings.autoEat() then
                local food = findFood(body)
                if food and eat(body, food, false) then
                    if Settings.diagnosticsEnabled() then print("[PersonalKnox][npc-eat] food npc=" .. tostring(body)) end
                end
            elseif thirst >= 0.60 and Settings.autoDrink() then
                local water = findWater(body)
                if water and eat(body, water, true) then
                    if Settings.diagnosticsEnabled() then print("[PersonalKnox][npc-eat] water npc=" .. tostring(body)) end
                end
            end
        end
    end
end

function NpcEat.install(adapters)
    deps = adapters
    Events.OnGameStart.Add(function() lastCheck = 0 end)
    Events.OnPlayerUpdate.Add(function()
        local player = getSpecificPlayer(0)
        if not player or player:isDead() then return end
        local ok, err = pcall(poll, player)
        if not ok then print("[PersonalKnox] NPC-eat error: " .. tostring(err)) end
    end)
end

return NpcEat
