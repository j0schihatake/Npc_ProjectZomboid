-- Equipment upkeep adapter: Java decides, Lua repairs and adds reserves.
local Upkeep = {}
local Settings = require "PersonalKnox/Settings"
local DURABILITY_MS, AMMO_MS = 5000, 10000
local STATE_MS = 60000
local lastDurability, lastAmmo, lastState = 0, 0, 0

local function api()
    return PersonalKnoxAPI and PersonalKnoxAPI.getInstance() or nil
end

local function repair(player)
    local java = api()
    for _, item in ipairs({player:getPrimaryHandItem(), player:getSecondaryHandItem()}) do
        if item and item:IsWeapon() then
            local condition, maximum = item:getCondition(), item:getConditionMax()
            if java:upkeepNeedsRepair(condition, maximum) then
                item:setConditionNoSound(maximum)
                pcall(function() if item:getSharpness() < 1 then item:setSharpness(1) end end)
                if Settings.diagnosticsEnabled() then
                    print("[PersonalKnox][upkeep] repair=" .. item:getFullType()
                        .. " " .. tostring(condition) .. "/" .. tostring(maximum))
                end
            end
        end
    end
end

local function countItems(container, fullType, depth)
    if not container or depth > 2 then return 0 end
    local total, items = 0, container:getItems()
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        if item:getFullType() == fullType then
            local count = 1
            pcall(function() count = item:getCount() end)
            total = total + count
        elseif item:IsInventoryContainer() then
            total = total + countItems(item:getInventory(), fullType, depth + 1)
        end
    end
    return total
end

local function ammo(player)
    local gun = player:getPrimaryHandItem()
    if not gun or not gun:IsWeapon() or not gun:isRanged() then return end
    local maximum = gun:getMaxAmmo()
    if not maximum or maximum <= 0 then return end
    if Settings.infiniteMagazine() then
        if gun.getCurrentAmmoCount and gun:getCurrentAmmoCount() < maximum and gun.setCurrentAmmoCount then
            gun:setCurrentAmmoCount(maximum)
            if Settings.diagnosticsEnabled() then print("[PersonalKnox][upkeep] magazine=" .. tostring(maximum)) end
        end
        return
    end
    local ammoType = gun:getAmmoType()
    if not ammoType then return end
    local key = ammoType:getItemKey()
    if not key or key == "" then return end
    local inventory = player:getInventory()
    local missing = api():upkeepAmmo(countItems(inventory, key, 0), maximum)
    if missing > 0 then
        inventory:AddItem(key, missing)
        if Settings.diagnosticsEnabled() then
            print("[PersonalKnox][upkeep] ammo=" .. key .. " add=" .. tostring(missing))
        end
    end
    local magazine = gun:getMagazineType()
    if magazine and magazine ~= "" and api():upkeepNeedsMagazine(countItems(inventory, magazine, 0)) then
        inventory:AddItem(magazine, 1)
    end
end

local function tick()
    if isClient() or not PersonalKnoxAPI or not classFilesInstalled then return end
    local player = getSpecificPlayer(0)
    if not player or player:isDead() then return end
    local now = getTimestampMs()
    if Settings.diagnosticsEnabled() and now - lastState >= STATE_MS then
        lastState = now
        local item = player:getPrimaryHandItem()
        local info = item and (item:getFullType() .. " " .. tostring(item:getCondition())
            .. "/" .. tostring(item:getConditionMax())) or "none"
        print("[PersonalKnox][upkeep-state] weapon=" .. info)
    end
    if Settings.infiniteDurability() and now - lastDurability >= DURABILITY_MS then
        lastDurability = now
        local ok, err = pcall(repair, player)
        if not ok then print("[PersonalKnox] Upkeep repair error: " .. tostring(err)) end
    end
    if Settings.infiniteAmmo() and now - lastAmmo >= AMMO_MS then
        lastAmmo = now
        local ok, err = pcall(ammo, player)
        if not ok then print("[PersonalKnox] Upkeep ammo error: " .. tostring(err)) end
    end
end

function Upkeep.install()
    Events.OnGameStart.Add(function() lastDurability, lastAmmo = 0, 0 end)
    Events.OnPlayerUpdate.Add(function()
        local ok, err = pcall(tick)
        if not ok then print("[PersonalKnox] Upkeep poll error: " .. tostring(err)) end
    end)
end

return Upkeep
