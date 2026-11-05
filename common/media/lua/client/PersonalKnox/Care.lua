-- Player self-care adapter: Java decides, Lua finds items and runs native actions.
require "TimedActions/ISTimedActionQueue"
require "TimedActions/ISApplyBandage"
require "TimedActions/ISTakePillAction"

local Care = {}
local Settings = require "PersonalKnox/Settings"
local INTERVAL_MS = 2000
local lastCheck = 0

local function api()
    return PersonalKnoxAPI and PersonalKnoxAPI.getInstance() or nil
end

local function stat(player, name)
    local ok, value = pcall(function() return player:getStats():get(name) end)
    if ok and type(value) == "number" then return value end
    return -1
end

local function maxPain(body)
    local parts = body:getBodyParts()
    local worst = 0
    for i = 0, parts:size() - 1 do
        local value = parts:get(i):getPain()
        if type(value) == "number" and value > worst then worst = value end
    end
    return worst
end

local function bleedingPart(player)
    local parts = player:getBodyDamage():getBodyParts()
    for i = 0, parts:size() - 1 do
        local part = parts:get(i)
        if part:bleeding() and not part:bandaged() then return part end
    end
end

local function findBandage(player)
    local items = player:getInventory():getItems()
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        local power = 0
        pcall(function() power = item:getBandagePower() end)
        if power > 0 then
            local dirty = false
            pcall(function() dirty = typeOf(item):find("Dirty", 1, true) ~= nil end)
            local infected = false
            pcall(function() infected = item:isInfected() end)
            if not dirty and not infected then return item end
        end
    end
end

local function findPainkiller(player)
    local items = player:getInventory():getItems()
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        local full = item:getFullType()
        if full == "Base.Pills" or (item:getType() and item:getType():find("Painkiller", 1, true)) then
            local uses = 0
            pcall(function() uses = item:getCurrentUsesFloat() end)
            if uses > 0 then return item end
        end
    end
end

local function findDrink(player)
    local items = player:getInventory():getItems()
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        local source = false
        pcall(function() source = item:isWaterSource() end)
        if source then
            local uses = 0
            pcall(function() uses = item:getCurrentUsesFloat() end)
            if uses > 0 then return item end
            local fluid = item:getFluidContainer()
            if fluid and fluid:getAmount() > 0 then return item end
        end
    end
end

local function findFood(player)
    local items = player:getInventory():getItems()
    local best, bestChange
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        if instanceof(item, "Food") then
            local change = item:getHungerChange()
            if change < 0 and not item:isRotten() and not item:isBurnt() then
                if not bestChange or change < bestChange then best, bestChange = item, change end
            end
        end
    end
    return best
end

local function busy(player)
    return player:hasTimedActions() or player:isSprinting() or player:isDraggingCorpse()
end

local function danger(player)
    return player:isAttacking() or player:isTargetedByZombie()
end

local function execute(player, action)
    if action == "bandage" then
        local part = bleedingPart(player)
        local item = part and findBandage(player)
        if not part or not item then return false end
        ISTimedActionQueue.add(ISApplyBandage:new(player, player, item, part, true))
        return true
    elseif action == "painkiller" then
        local item = findPainkiller(player)
        if not item then return false end
        ISTimedActionQueue.add(ISTakePillAction:new(player, item))
        return true
    elseif action == "drink" then
        local item = findDrink(player)
        return item ~= nil and player:DrinkFluid(item, 0.5, false) == true
    elseif action == "eat" then
        local item = findFood(player)
        return item ~= nil and player:Eat(item, 1.0, false) == true
    end
    return false
end

local function poll()
    if isClient() or not PersonalKnoxAPI or not classFilesInstalled then return end
    local player = getSpecificPlayer(0)
    if not player or player:isDead() then return end
    local now = getTimestampMs()
    if now - lastCheck < INTERVAL_MS then return end
    lastCheck = now
    local body = player:getBodyDamage()
    local action = api():careAction(now, body:getHealth(), body:getNumPartsBleeding(),
        maxPain(body), stat(player, CharacterStat.THIRST), stat(player, CharacterStat.HUNGER),
        busy(player), danger(player), Settings.autoMedic(), Settings.autoDrink(), Settings.autoEat())
    if action == "none" then return end
    local ok, done = pcall(execute, player, action)
    if Settings.diagnosticsEnabled() then
        print("[PersonalKnox][care] action=" .. action .. " ok=" .. tostring(ok and done)
            .. " bleeding=" .. tostring(body:getNumPartsBleeding())
            .. " pain=" .. string.format("%.1f", maxPain(body))
            .. " thirst=" .. string.format("%.2f", stat(player, CharacterStat.THIRST))
            .. " hunger=" .. string.format("%.2f", stat(player, CharacterStat.HUNGER)))
    end
    if ok and done then
        api():careDone(action, getTimestampMs())
    else
        api():careUnavailable(action, getTimestampMs())
        if not ok then print("[PersonalKnox] Care error: " .. tostring(done)) end
    end
end

function Care.install()
    Events.OnGameStart.Add(function()
        lastCheck = 0
        if PersonalKnoxAPI then PersonalKnoxAPI.getInstance():resetCare() end
    end)
    Events.OnPlayerUpdate.Add(function()
        local ok, err = pcall(poll)
        if not ok then print("[PersonalKnox] Care poll error: " .. tostring(err)) end
    end)
end

return Care
