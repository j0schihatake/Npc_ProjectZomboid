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
    check("follow-order", function()
        api:ownCommand(id, "FOLLOW", x, y, z)
        return string.find(api:ownSummary(id), "order=FOLLOW", 1, true) ~= nil
    end)
    check("hold-order", function()
        api:ownCommand(id, "HOLD", x, y, z)
        return string.find(api:ownSummary(id), "order=HOLD", 1, true) ~= nil
    end)
    check("loot-order", function()
        api:ownCommand(id, "LOOT", x, y, z)
        return string.find(api:ownSummary(id), "order=LOOT", 1, true) ~= nil
    end)
    check("tick", function() return api:ownTick(0.01, 8) >= 0 end)
    check("target-floor", function() return api:ownTargetZSnapshot() ~= nil and api:ownTargetZSnapshot() ~= "" end)
    check("body-inspect", function() return string.find(api:ownBodyInspect(id), "id=" .. id, 1, true) ~= nil end)
    check("alife-squads", function() return api:alifeSquads() >= 1 end)
    print("[PersonalKnox][ingame-test] summary passed=" .. tostring(passed) .. " failed=" .. tostring(failed))
    return failed == 0
end

return Tests
