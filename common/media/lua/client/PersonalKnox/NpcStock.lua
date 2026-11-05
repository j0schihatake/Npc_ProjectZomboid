-- Small personal reserve: food, water and a can opener; refill before need.
local NpcStock = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local INTERVAL_MS, MAX_NPCS = 30000, 2
local TARGET_FOOD, TARGET_WATER, TARGET_OPENER = 2, 2, 1
local lastCheck, deps = 0, nil

local function isWater(item)
    local fluid = item:getFluidContainer()
    if not fluid or fluid:getAmount() <= 0 then return false end
    local primary = fluid:getPrimaryFluid()
    return primary and primary:getFluidTypeString() == "Water"
end

local function isOpener(item)
    local kind = (item:getType() or ""):lower()
    return kind:find("canopener", 1, true) or kind:find("tinopener", 1, true)
        or kind:find("bottleopener", 1, true)
end

local function counts(body)
    local food, water, opener = 0, 0, 0
    local items = body:getInventory():getItems()
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        if item:IsFood() then food = food + 1
        elseif isWater(item) then water = water + 1
        elseif isOpener(item) then opener = opener + 1 end
    end
    return food, water, opener
end

local function needed(body)
    local food, water, opener = counts(body)
    if food < TARGET_FOOD then return "food" end
    if water < TARGET_WATER then return "water" end
    if opener < TARGET_OPENER then return "opener" end
    return nil
end

local function nearestSource(body, kind)
    local cell = getCell()
    local px, py, pz = math.floor(body:getX()), math.floor(body:getY()), math.floor(body:getZ())
    local best, bestContainer, bestDistance = nil, nil, math.huge
    for dx = -6, 6 do
        for dy = -6, 6 do
            local square = cell:getGridSquare(px + dx, py + dy, pz)
            if square then
                local objects = square:getObjects()
                for i = 0, objects:size() - 1 do
                    local object = objects:get(i)
                    local container = object.getContainer and object:getContainer()
                    if container then
                        local items = container:getItems()
                        for j = 0, items:size() - 1 do
                            local item = items:get(j)
                            local match = (kind == "food" and item:IsFood())
                                or (kind == "water" and isWater(item))
                                or (kind == "opener" and isOpener(item))
                            if match then
                                local distance = dx * dx + dy * dy
                                if distance < bestDistance then
                                    best, bestContainer, bestDistance = item, container, distance
                                end
                            end
                        end
                    end
                end
            end
        end
    end
    return best, bestContainer, bestDistance
end

local function poll(player)
    if isClient() or not deps or Perf.skip() or not Settings.npcStock() then return end
    local now = getTimestampMs()
    if now - lastCheck < INTERVAL_MS then return end
    lastCheck = now
    local checked = 0
    for _, npc in ipairs(deps.roster(player)) do
        if checked >= MAX_NPCS then break end
        local body = npc:getWorldObject()
        if body and not body:isDead() and not body:hasTimedActions() then
            checked = checked + 1
            local kind = needed(body)
            if kind then
                local item, container, distance = nearestSource(body, kind)
                if item and container and ISInventoryTransferAction and ISTimedActionQueue then
                    ISTimedActionQueue.add(ISInventoryTransferAction:new(body, item, container, body:getInventory()))
                    if Settings.diagnosticsEnabled() then
                        print("[PersonalKnox][npc-stock] " .. kind .. "=" .. tostring(item:getType())
                            .. " distance=" .. tostring(distance))
                    end
                end
            end
        end
    end
end

function NpcStock.install(adapters)
    deps = adapters
    Events.OnGameStart.Add(function() lastCheck = 0 end)
    Events.OnPlayerUpdate.Add(function()
        local player = getSpecificPlayer(0)
        if not player or player:isDead() then return end
        local ok, err = pcall(poll, player)
        if not ok then print("[PersonalKnox] NPC-stock error: " .. tostring(err)) end
    end)
end

return NpcStock
