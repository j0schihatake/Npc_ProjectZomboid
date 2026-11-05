-- A surrounded companion shouts for help, gated by the help-frequency setting.
local HelpCall = {}
local Help = require "PersonalKnox/Help"
local Call = require "PersonalKnox/Call"
local Settings = require "PersonalKnox/Settings"
local INTERVAL_MS, RADIUS, MAX_NPCS, MAX_OBJECTS = 5000, 4, 4, 128
local lastCheck = 0
local lastScan = 0
local assist = {}

local function threats(body)
    local cell = getCell()
    local px, py, pz = math.floor(body:getX()), math.floor(body:getY()), math.floor(body:getZ())
    local count, processed = 0, 0
    for dx = -RADIUS, RADIUS do
        for dy = -RADIUS, RADIUS do
            local square = cell:getGridSquare(px + dx, py + dy, pz)
            if square then
                local objects = square:getMovingObjects()
                for i = 0, objects:size() - 1 do
                    processed = processed + 1
                    local object = objects:get(i)
                    if instanceof(object, "IsoZombie") and not object:isDead() and body:CanSee(object) then
                        count = count + 1
                    end
                    if processed >= MAX_OBJECTS then return count end
                end
            end
        end
    end
    return count
end

local function poll(player, deps)
    if isClient() or not PersonalKnoxAPI or not Settings.speechEnabled() then return end
    local now = getTimestampMs()
    if now - lastCheck < INTERVAL_MS then return end
    lastCheck = now
    local best, bestCount, checked = nil, 0, 0
    local maxNpc, maxCount = nil, 0
    for _, npc in ipairs(deps.roster(player)) do
        if checked >= MAX_NPCS then break end
        local body = npc:getWorldObject()
        if body and not body:isDead() then
            local dx, dy = body:getX() - player:getX(), body:getY() - player:getY()
            if dx * dx + dy * dy <= 900 then
                checked = checked + 1
                local count = threats(body)
                if count > maxCount then maxNpc, maxCount = npc, count end
                if count >= 3 and count > bestCount then best, bestCount = npc, count end
            end
        end
    end
    for ally, untilAt in pairs(assist) do
        if now >= untilAt then
            assist[ally] = nil
            Call.one(player, ally, true)
        end
    end
    if Settings.diagnosticsEnabled() and now - lastScan >= 60000 then
        lastScan = now
        print("[PersonalKnox][help-scan] npc=" .. tostring(maxNpc and maxNpc:getId() or "none")
            .. " threats=" .. tostring(maxCount))
    end
    if best and Help.allowed(now) then
        deps.say(player, best, "react_horde")
        if Settings.diagnosticsEnabled() then
            print("[PersonalKnox][help-call] npc=" .. tostring(best:getId()) .. " zombies=" .. tostring(bestCount))
        end
        local bestBody = best:getWorldObject()
        local sent = 0
        for _, ally in ipairs(deps.roster(player)) do
            if sent >= 2 then break end
            if ally ~= best and not assist[ally] and tellNpcStay then
                local body = ally:getWorldObject()
                if body and not body:isDead() and not body:hasTimedActions() and not body:isAttacking() then
                    local dx, dy = body:getX() - bestBody:getX(), body:getY() - bestBody:getY()
                    if dx * dx + dy * dy <= 400 then
                        local ok = pcall(tellNpcStay, ally, bestBody:getX(), bestBody:getY(), bestBody:getZ())
                        if ok then
                            assist[ally] = now + 20000
                            sent = sent + 1
                            if Settings.diagnosticsEnabled() then
                                print("[PersonalKnox][help-rush] npc=" .. tostring(ally:getId())
                                    .. " to=" .. tostring(best:getId()))
                            end
                        end
                    end
                end
            end
        end
    end
end

function HelpCall.install(deps)
    Events.OnGameStart.Add(function() lastCheck = 0 end)
    Events.OnPlayerUpdate.Add(function()
        local player = getSpecificPlayer(0)
        if not player or player:isDead() then return end
        local ok, err = pcall(poll, player, deps)
        if not ok then print("[PersonalKnox] Help-call error: " .. tostring(err)) end
    end)
end

return HelpCall
