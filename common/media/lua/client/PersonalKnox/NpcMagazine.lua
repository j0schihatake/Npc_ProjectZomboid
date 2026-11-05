-- Infinite-magazine mode for companions: keep their firearm magazine full.
local NpcMagazine = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local INTERVAL_MS, MAX_NPCS = 5000, 2
local lastCheck, deps = 0, nil

local function poll(player)
    if isClient() or not deps or Perf.skip() or not Settings.infiniteMagazine() then return end
    local now = getTimestampMs()
    if now - lastCheck < INTERVAL_MS then return end
    lastCheck = now
    local checked = 0
    for _, npc in ipairs(deps.roster(player)) do
        if checked >= MAX_NPCS then break end
        local body = npc:getWorldObject()
        if body and not body:isDead() then
            checked = checked + 1
            local weapon = body:getPrimaryHandItem()
            if weapon and instanceof(weapon, "HandWeapon")
                    and weapon.getCurrentAmmoCount and weapon.getMaxAmmo and weapon.setCurrentAmmoCount then
                local max = weapon:getMaxAmmo()
                if max > 0 and weapon:getCurrentAmmoCount() < max then
                    weapon:setCurrentAmmoCount(max)
                    if Settings.diagnosticsEnabled() then
                        print("[PersonalKnox][npc-magazine] npc=" .. tostring(body) .. " max=" .. tostring(max))
                    end
                end
            end
        end
    end
end

function NpcMagazine.install(adapters)
    deps = adapters
    Events.OnGameStart.Add(function() lastCheck = 0 end)
    Events.OnPlayerUpdate.Add(function()
        local player = getSpecificPlayer(0)
        if not player or player:isDead() then return end
        local ok, err = pcall(poll, player)
        if not ok then print("[PersonalKnox] NPC-magazine error: " .. tostring(err)) end
    end)
end

return NpcMagazine
