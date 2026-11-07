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
