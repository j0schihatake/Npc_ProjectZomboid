-- Companions reload their own firearms when the magazine is not full.
local NpcReload = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local Util = require "PersonalKnox/Util"
local INTERVAL_MS, MAX_NPCS = 5000, 2
local lastCheck, deps = 0, nil

local function poll(player)
    if isClient() or not deps or Perf.essentialSkip() or Settings.infiniteMagazine() or not Settings.autoReload() then return end
    local now = getTimestampMs()
    if now - lastCheck < INTERVAL_MS then return end
    lastCheck = now
    local checked = 0
    for _, npc in ipairs(deps.roster(player)) do
        if checked >= MAX_NPCS then break end
        local body = npc:getWorldObject()
        if body and not body:isDead() and not body:hasTimedActions() and not body:getVehicle() then
            checked = checked + 1
            local weapon = body:getPrimaryHandItem()
            if weapon and instanceof(weapon, "HandWeapon") then
                local current, max = 0, 0
                if weapon.getCurrentAmmoCount then current = weapon:getCurrentAmmoCount() end
                if weapon.getMaxAmmo then max = weapon:getMaxAmmo() end
                local ammoType = nil
                if weapon.getAmmoType then ammoType = weapon:getAmmoType() end
                local reserve = 0
                if ammoType and ammoType.getItemKey then
                    reserve = Util.countAmmo(body:getInventory(), ammoType:getItemKey())
                end
                local reloadable = true
                if weapon.isReloadable then
                    local ok, result = pcall(function() return weapon:isReloadable(body) end)
                    reloadable = ok and result
                end
                if reloadable and max > 0 and current < max and reserve > 0
                        and (not Settings.reloadOnlyWhenEmpty() or current == 0)
                        and ISReloadWeaponAction and ISTimedActionQueue then
                    ISTimedActionQueue.add(ISReloadWeaponAction:new(body, weapon))
                    if Settings.diagnosticsEnabled() then
                        print("[PersonalKnox][npc-reload] start " .. tostring(current) .. "/" .. tostring(max))
                    end
                end
            end
        end
    end
end

function NpcReload.install(adapters)
    deps = adapters
    Events.OnGameStart.Add(function() lastCheck = 0 end)
    Events.OnPlayerUpdate.Add(function()
        local player = getSpecificPlayer(0)
        if not player or player:isDead() then return end
        local ok, err = pcall(poll, player)
        if not ok then print("[PersonalKnox] NPC-reload error: " .. tostring(err)) end
    end)
end

return NpcReload
