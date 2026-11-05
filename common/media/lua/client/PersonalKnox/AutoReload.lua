-- Native auto-reload for the player; NPCs get their own later.
local AutoReload = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local Util = require "PersonalKnox/Util"
local INTERVAL_MS = 1000
local lastCheck = 0

local function poll(player)
    if isClient() or Perf.skip() or Settings.infiniteMagazine() or not Settings.autoReload() then return end
    local now = getTimestampMs()
    if now - lastCheck < INTERVAL_MS then return end
    lastCheck = now
    if player:hasTimedActions() or player:getVehicle() then return end
    local weapon = player:getPrimaryHandItem()
    if not weapon or not instanceof(weapon, "HandWeapon") then return end
    local current, max = 0, 0
    if weapon.getCurrentAmmoCount then current = weapon:getCurrentAmmoCount() end
    if weapon.getMaxAmmo then max = weapon:getMaxAmmo() end
    local reloadable = true
    if weapon.isReloadable then
        local ok, result = pcall(function() return weapon:isReloadable(player) end)
        reloadable = ok and result
    end
    if not reloadable or max <= 0 or current >= max then return end
    local ammoType = nil
    if weapon.getAmmoType then ammoType = weapon:getAmmoType() end
    local reserve = 0
    if ammoType and ammoType.getItemKey then
        reserve = Util.countAmmo(player:getInventory(), ammoType:getItemKey())
    end
    if reserve <= 0 then return end
    if ISReloadWeaponAction and ISTimedActionQueue then
        ISTimedActionQueue.add(ISReloadWeaponAction:new(player, weapon))
        if Settings.diagnosticsEnabled() then
            print("[PersonalKnox][reload] start " .. tostring(current) .. "/" .. tostring(max))
        end
    end
end

function AutoReload.install()
    Events.OnGameStart.Add(function() lastCheck = 0 end)
    Events.OnPlayerUpdate.Add(function()
        local player = getSpecificPlayer(0)
        if not player or player:isDead() then return end
        local ok, err = pcall(poll, player)
        if not ok then print("[PersonalKnox] Auto-reload error: " .. tostring(err)) end
    end)
end

return AutoReload
