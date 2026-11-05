-- Ammo weight multiplier: 0% / 50% / 100% of the original, never compounding.
local AmmoWeight = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"

local INTERVAL_MS, LOG_MS = 15000, 60000
local last, lastLog, logged = 0, 0, false
local deps

local function isAmmo(item)
    local ok, category = pcall(function() return item:getDisplayCategory() end)
    if ok and category == "Ammo" then return true end
    local ok2, kind = pcall(function() return item:getType() end)
    if not ok2 or type(kind) ~= "string" then return false end
    kind = kind:lower()
    return kind:find("bullet", 1, true) ~= nil or kind:find("ammo", 1, true) ~= nil
        or kind:find("shell", 1, true) ~= nil
end

local function apply(item)
    local data = item:getModData()
    local base = data.PersonalKnoxAmmoBaseWeight
    if type(base) ~= "number" or base <= 0 then
        base = item:getActualWeight()
        data.PersonalKnoxAmmoBaseWeight = base
    end
    local target = base * Settings.ammoWeightFactor()
    if math.abs(item:getActualWeight() - target) > 0.0001 then
        item:setActualWeight(target)
    end
end

local function scan(inventory)
    local items = inventory:getItems()
    for i = 0, math.min(items:size(), 256) - 1 do
        local item = items:get(i)
        local ok, err = pcall(function()
            if isAmmo(item) then apply(item) end
        end)
        if not ok then print("[PersonalKnox] Ammo-weight error: " .. tostring(err)) end
    end
end

local function tick()
    local player = getSpecificPlayer(0)
    if not player or player:isDead() then return end
    local now = getTimestampMs()
    if now - last < INTERVAL_MS then return end
    last = now
    if Perf.skip() then return end
    scan(player:getInventory())
    local count = 0
    if deps and deps.roster then
        for _, npc in ipairs(deps.roster(player)) do
            if count >= 8 then break end
            local body = npc:getWorldObject()
            if body and not body:isDead() then
                scan(body:getInventory())
                count = count + 1
            end
        end
    end
    if not logged then
        logged = true
        print("[PersonalKnox][ammo-weight] player+squad applied factor=" .. tostring(Settings.ammoWeightFactor()))
    elseif Settings.diagnosticsEnabled() and now - lastLog >= LOG_MS then
        lastLog = now
        print("[PersonalKnox][ammo-weight] refreshed squad=" .. tostring(count))
    end
end

function AmmoWeight.install(adapters)
    deps = adapters
    Events.OnPlayerUpdate.Add(function()
        local ok, err = pcall(tick)
        if not ok then print("[PersonalKnox] Ammo-weight poll error: " .. tostring(err)) end
    end)
end

return AmmoWeight
