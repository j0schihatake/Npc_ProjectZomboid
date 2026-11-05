-- Player self-care adapter: Java decides, Lua finds items and runs native actions.
pcall(require, "TimedActions/ISTimedActionQueue")
pcall(require, "TimedActions/ISApplyBandage")
pcall(require, "TimedActions/ISTakePillAction")
pcall(require, "TimedActions/ISEatFoodAction")
pcall(require, "TimedActions/ISDrinkFluidAction")
pcall(require, "TimedActions/ISInventoryTransferAction")

local Care = {}
local Settings = require "PersonalKnox/Settings"
local INTERVAL_MS = 2000
local STATE_MS = 60000
local lastCheck = 0
local lastState = 0
local initLogged = false

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

local function woundPart(player)
    local parts = player:getBodyDamage():getBodyParts()
    local injured
    for i = 0, parts:size() - 1 do
        local part = parts:get(i)
        if not part:bandaged() then
            if part:bleeding() then return part end
            if part:HasInjury() and not injured then injured = part end
        end
    end
    return injured
end

local function woundCount(player)
    local count, parts = 0, player:getBodyDamage():getBodyParts()
    for i = 0, parts:size() - 1 do
        local part = parts:get(i)
        if not part:bandaged() and (part:bleeding() or part:HasInjury()) then count = count + 1 end
    end
    return count
end

local function scan(container, predicate, depth, budget)
    if not container or depth > 2 then return nil end
    local items = container:getItems()
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        budget.n = budget.n + 1
        if budget.n > 200 then return nil end
        if predicate(item) then return item end
        if item:IsInventoryContainer() then
            local found = scan(item:getInventory(), predicate, depth + 1, budget)
            if found then return found end
        end
    end
end

local function findItem(player, predicate)
    local items = player:getInventory():getItems()
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        if predicate(item) then return item end
    end
    if not Settings.careSearchBags() then return nil end
    local budget = {n = 0}
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        if item:IsInventoryContainer() then
            local found = scan(item:getInventory(), predicate, 1, budget)
            if found then return found end
        end
    end
end

local function findBandage(player)
    local best, bestScore = nil, 0
    findItem(player, function(item)
        local power = 0
        pcall(function() power = item:getBandagePower() end)
        local canBandage = false
        pcall(function() canBandage = item:isCanBandage() end)
        if power <= 0 and not canBandage then return false end
        local infected = false
        pcall(function() infected = item:isInfected() end)
        if infected then return false end
        local dirty = false
        pcall(function() dirty = typeOf(item):find("Dirty", 1, true) ~= nil end)
        local score = dirty and 1 or 2
        if score > bestScore then best, bestScore = item, score end
        return false
    end)
    return best
end

local function findPainkiller(player)
    return findItem(player, function(item)
        local full = item:getFullType()
        if full ~= "Base.Pills" and not (item:getType() and item:getType():find("Painkiller", 1, true)) then return false end
        local uses = 0
        pcall(function() uses = item:getCurrentUsesFloat() end)
        return uses > 0
    end)
end

local function findDrink(player)
    return findItem(player, function(item)
        local source = false
        pcall(function() source = item:isWaterSource() end)
        if not source then return false end
        local uses = 0
        pcall(function() uses = item:getCurrentUsesFloat() end)
        if uses > 0 then return true end
        local fluid = item:getFluidContainer()
        return fluid ~= nil and fluid:getAmount() > 0
    end)
end

local function findFood(player)
    return findItem(player, function(item)
        return instanceof(item, "Food") and item:getHungerChange() < 0
            and not item:isRotten() and not item:isBurnt()
    end)
end

local function ensureMain(player, item)
    local inventory = player:getInventory()
    if item:getContainer() and item:getContainer() ~= inventory then
        ISTimedActionQueue.add(ISInventoryTransferAction:new(player, item, item:getContainer(), inventory, 10))
    end
end

local function busy(player)
    return player:hasTimedActions() or player:isSprinting() or player:isDraggingCorpse()
end

local function danger(player)
    return player:isAttacking() or player:isTargetedByZombie()
end

local function execute(player, action)
    if action == "bandage" then
        local part = woundPart(player)
        local item = part and findBandage(player)
        if not part or not item then return false end
        ensureMain(player, item)
        ISTimedActionQueue.add(ISApplyBandage:new(player, player, item, part, true))
        return true
    elseif action == "painkiller" then
        local item = findPainkiller(player)
        if not item then return false end
        ensureMain(player, item)
        ISTimedActionQueue.add(ISTakePillAction:new(player, item))
        return true
    elseif action == "drink" then
        local item = findDrink(player)
        if not item then return false end
        ensureMain(player, item)
        ISTimedActionQueue.add(ISDrinkFluidAction:new(player, item, 0.5))
        return true
    elseif action == "eat" then
        local item = findFood(player)
        if not item then return false end
        ensureMain(player, item)
        ISTimedActionQueue.add(ISEatFoodAction:new(player, item, 1))
        return true
    end
    return false
end

local function poll()
    if not initLogged then
        initLogged = true
        print("[PersonalKnox][care-init] classFilesInstalled=" .. tostring(classFilesInstalled)
            .. " api=" .. tostring(PersonalKnoxAPI ~= nil)
            .. " diagnostics=" .. tostring(Settings.diagnosticsEnabled()))
    end
    if isClient() or not PersonalKnoxAPI or not classFilesInstalled then return end
    local player = getSpecificPlayer(0)
    if not player or player:isDead() then return end
    local now = getTimestampMs()
    if now - lastCheck < INTERVAL_MS then return end
    lastCheck = now
    local body = player:getBodyDamage()
    local action = api():careAction(now, body:getHealth(), woundCount(player),
        maxPain(body), stat(player, CharacterStat.THIRST), stat(player, CharacterStat.HUNGER),
        busy(player), danger(player), Settings.autoMedic(), Settings.autoDrink(), Settings.autoEat())
    if Settings.diagnosticsEnabled() and now - lastState >= STATE_MS then
        lastState = now
        print("[PersonalKnox][care-state] health=" .. string.format("%.1f", body:getHealth())
            .. " wounds=" .. tostring(woundCount(player))
            .. " pain=" .. string.format("%.1f", maxPain(body))
            .. " thirst=" .. string.format("%.2f", stat(player, CharacterStat.THIRST))
            .. " hunger=" .. string.format("%.2f", stat(player, CharacterStat.HUNGER))
            .. " action=" .. action)
    end
    if action == "none" then return end
    local ok, done = pcall(execute, player, action)
    if Settings.diagnosticsEnabled() then
        print("[PersonalKnox][care] action=" .. action .. " ok=" .. tostring(ok and done)
            .. " wounds=" .. tostring(woundCount(player))
            .. " pain=" .. string.format("%.1f", maxPain(body))
            .. " thirst=" .. string.format("%.2f", stat(player, CharacterStat.THIRST))
            .. " hunger=" .. string.format("%.2f", stat(player, CharacterStat.HUNGER)))
    end
    if ok and done then
        api():careDone(action, getTimestampMs())
    else
        api():careUnavailable(action, getTimestampMs())
        if not ok then
            print("[PersonalKnox] Care error: " .. tostring(done))
        elseif Settings.diagnosticsEnabled() then
            print("[PersonalKnox][care] unavailable=" .. action .. " (no suitable item or action refused)")
        end
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
