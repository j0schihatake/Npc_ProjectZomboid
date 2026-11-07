local Settings = require "PersonalKnox/Settings"
-- In-game obstacle course for the own-NPC layer. Run from Mod Options or F5.
local Tests = {}
local passed, failed = 0, 0

local function check(name, fn)
    local ok, result = pcall(fn)
    if ok and result ~= false then
        passed = passed + 1
        print("[PersonalKnox][ingame-test] PASS " .. name)
    else
        failed = failed + 1
        print("[PersonalKnox][ingame-test] FAIL " .. name .. " :: " .. tostring(result))
    end
end

function Tests.run(player)
    if not PersonalKnoxAPI or not player then return end
    passed, failed = 0, 0
    local api = PersonalKnoxAPI.getInstance()
    local x, y, z = player:getX(), player:getY(), player:getZ()
    print("[PersonalKnox][ingame-test] start")
    check("class-files", function() return classFilesInstalled == true end)
    check("player-clothed", function()
        local worn = player:getWornItems()
        return worn ~= nil and worn:size() >= 3
    end)
    local id = api:ownCreateCompanion(player)
    local bodies = api:ownBodiesSync()
    check("spawn-returns-id", function() return id ~= nil and id ~= "" end)
    check("body-sync", function() return bodies >= 1 end)
    check("body-registered", function()
        api:ownBodiesSync()
        local inspect = api:ownBodyInspect(id)
        return string.find(inspect, "squareMember=true", 1, true) ~= nil
    end)
    check("summary", function() return string.find(api:ownSummary(id), "id=" .. id, 1, true) ~= nil end)
    check("tools", function() return string.find(api:ownBodyTools(id), "HAMMER", 1, true) ~= nil end)
    check("clothing", function()
        local worn = api:ownBodyWorn(id)
        print("[PersonalKnox][ingame-test] worn=" .. tostring(worn))
        return string.find(worn, "Tshirt", 1, true) ~= nil
                and string.find(worn, "Trousers", 1, true) ~= nil
                and string.find(worn, "Shoes", 1, true) ~= nil
    end)
    check("follow-order", function()
        api:ownCommand(id, "FOLLOW", x, y, z)
        return string.find(api:ownSummary(id), "order=FOLLOW", 1, true) ~= nil
    end)
    check("hold-order", function()
        api:ownCommand(id, "HOLD", x, y, z)
        return string.find(api:ownSummary(id), "order=HOLD", 1, true) ~= nil
    end)
    check("rest-order", function()
        api:ownCommand(id, "REST", x, y, z)
        return string.find(api:ownSummary(id), "order=REST", 1, true) ~= nil
    end)
    check("loot-order", function()
        api:ownCommand(id, "LOOT", x, y, z)
        return string.find(api:ownSummary(id), "order=LOOT", 1, true) ~= nil
    end)
    check("tick", function() return api:ownTick(0.01, 8) >= 0 end)
    check("target-floor", function() return api:ownTargetZSnapshot() ~= nil and api:ownTargetZSnapshot() ~= "" end)
    check("body-inspect", function() return string.find(api:ownBodyInspect(id), "id=" .. id, 1, true) ~= nil end)
    check("damage", function()
        local before = tonumber((api:ownSummary(id):match("health=([%d%.]+)")) or "0")
        api:ownDamage(id, 10)
        local after = tonumber((api:ownSummary(id):match("health=([%d%.]+)")) or "0")
        return after < before
    end)
    check("speak", function() return api:ownSpeakById(id, "Тест связи.") end)
    check("base", function() return string.find(api:ownBaseSet(x, y, z), "base=", 1, true) ~= nil end)
    check("base-guard", function() return api:ownBaseGuardAll() >= 1 end)
    check("body-model", function()
        local inspect = api:ownBodyInspect(id)
        return string.find(inspect, "model=true", 1, true) ~= nil
                and string.find(inspect, "render=true", 1, true) ~= nil
    end)
    check("tools-all", function()
        local tools = api:ownBodyTools(id)
        return string.find(tools, "HAMMER", 1, true) ~= nil
                and string.find(tools, "SCREWDRIVER", 1, true) ~= nil
                and string.find(tools, "SAW", 1, true) ~= nil
    end)
    check("snapshot", function() return api:ownSnapshot() ~= nil and api:ownSnapshot() ~= "" end)
    check("population-snapshot", function() return api:ownPopulationSnapshot() ~= nil and api:ownPopulationSnapshot() ~= "" end)
    check("base-summary", function() return string.find(api:ownBaseSummary(), "base=", 1, true) ~= nil end)
    check("range-clear", function() return api:ownCombatRange(10, false, "clear") == 10 end)
    check("range-scope", function() return api:ownCombatRange(10, true, "clear") == 15 end)
    check("range-rain", function() return api:ownCombatRange(10, false, "rain") == 6.5 end)
    check("range-storm", function() return api:ownCombatRange(10, false, "storm") == 4.5 end)
    check("damage-weapon", function() return api:ownCombatDamage(1, 0, 10, 10, false) == 10 end)
    check("damage-skill", function() return api:ownCombatDamage(1, 3, 10, 10, false) == 16 end)
    check("damage-broken", function() return api:ownCombatDamage(1, 0, 0, 10, false) == 5 end)
    check("damage-ranged", function() return api:ownCombatDamage(5, 5, 10, 10, true) == 30 end)
    check("factions", function() return api:alifeOutposts() >= 1 end)
    check("combat", function()
        local spawned = api:ownDebugZombie(id)
        if string.find(spawned, "health=", 1, true) == nil then return false end
        local before = api:ownDebugZombieHealth()
        api:ownDebugAttack(id)
        local damaged = api:ownDebugZombieHealth() < before
        api:ownDebugZombieClear()
        return damaged
    end)
    check("ranged-combat", function()
        local spawned = api:ownDebugZombie(id)
        if string.find(spawned, "health=", 1, true) == nil then return false end
        local equip = api:ownDebugEquip(id, "Base.Pistol", 5)
        if string.find(equip, "equipped=", 1, true) == nil then return false end
        local before = api:ownDebugZombieHealth()
        api:ownDebugAttack(id)
        local damaged = api:ownDebugZombieHealth() < before
        local ammo = api:ownDebugAmmo(id)
        api:ownDebugZombieClear()
        return damaged and ammo == 4
    end)
    check("upkeep-repair", function() return api:upkeepNeedsRepair(1, 10) end)
    check("upkeep-ammo", function() return api:upkeepAmmo(0, 30) > 0 end)
    check("durability-setting", function() return Settings.infiniteDurability() == true end)
    check("command-all", function() return api:ownCommandAll("REST", x, y, z) >= 1 end)
    check("command-all-follow", function() return api:ownCommandAll("FOLLOW", x, y, z) >= 1 end)
    check("retreat-away", function()
        local p = api:ownRetreatPoint(1, 0, 0, 0, 6)
        return string.find(p, "x=3.00", 1, true) ~= nil
    end)
    check("retreat-hold", function()
        local p = api:ownRetreatPoint(8, 0, 0, 0, 6)
        return string.find(p, "x=8.00", 1, true) ~= nil
    end)
    check("squad-panel", function()
        local ok, panel = pcall(require, "PersonalKnox/OwnNpcPanel")
        if not ok or not panel then return false end
        panel.toggle(player)
        local open = panel.isOpen()
        panel.closeOpen()
        return open
    end)
    check("dismantle", function()
        local obstacle = api:ownDebugObstacle(id)
        if obstacle == nil or obstacle == "" then return false end
        local started = api:ownDismantleStart(id)
        if started == nil or started == "" then return false end
        api:ownDismantleTick(id, 30)
        return true
    end)
    check("give-items", function()
        api:ownDebugEquip(id, "Base.Pistol", 0)
        return api:ownGiveItems(id, player) >= 1
    end)
    check("remove", function()
        local before = api:ownActive()
        api:ownRemove(id)
        return api:ownActive() == before - 1
    end)
    check("alife-squads", function() return api:alifeSquads() >= 1 end)
    print("[PersonalKnox][ingame-test] summary passed=" .. tostring(passed) .. " failed=" .. tostring(failed))
    return failed == 0
end

return Tests
