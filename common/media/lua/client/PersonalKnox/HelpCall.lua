-- A surrounded companion shouts for help, gated by the help-frequency setting.
local HelpCall = {}
local Help = require "PersonalKnox/Help"
local Call = require "PersonalKnox/Call"
local Settings = require "PersonalKnox/Settings"
local INTERVAL_MS, RADIUS, MAX_NPCS, MAX_OBJECTS = 5000, 4, 4, 128
local lastCheck = 0
local lastScan = 0
local assist = {}
local retreating = {}
local lastRequest = nil

local function threats(body, radius)
    radius = radius or RADIUS
    local cell = getCell()
    local px, py, pz = math.floor(body:getX()), math.floor(body:getY()), math.floor(body:getZ())
    local count, processed = 0, 0
    for dx = -radius, radius do
        for dy = -radius, radius do
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
    for ally, state in pairs(assist) do
        local targetBody = state.target and state.target:getWorldObject()
        local close = targetBody and not targetBody:isDead() and threats(targetBody, RADIUS) or 0
        local incoming = targetBody and not targetBody:isDead() and threats(targetBody, 12) or 0
        if targetBody and not targetBody:isDead() and close < 3 and incoming > 0
                and not retreating[state.target] then
            retreating[state.target] = now + 10000
            Call.one(player, state.target, true)
            lastRequest = nil
            if Settings.diagnosticsEnabled() then
                print("[PersonalKnox][help-fallback] npc=" .. tostring(state.target:getId())
                    .. " incoming=" .. tostring(incoming))
            end
        end
        if now >= state.untilAt or not targetBody or targetBody:isDead() or close < 3 then
            assist[ally] = nil
            Call.one(player, ally, true)
            if Settings.diagnosticsEnabled() then
                print("[PersonalKnox][help-retreat] npc=" .. tostring(ally:getId()))
            end
        end
    end
    if Settings.diagnosticsEnabled() and now - lastScan >= 60000 then
        lastScan = now
        print("[PersonalKnox][help-scan] npc=" .. tostring(maxNpc and maxNpc:getId() or "none")
            .. " threats=" .. tostring(maxCount))
    end
    if best and Help.allowed(now) then
        deps.say(player, best, "help_call")
        lastRequest = {npc = best, untilAt = now + 20000}
        if Settings.diagnosticsEnabled() then
            print("[PersonalKnox][help-call] npc=" .. tostring(best:getId()) .. " zombies=" .. tostring(bestCount))
        end
        local request = best
        local requestBody = lastRequest.npc:getWorldObject()
        if lastRequest.npc ~= best and requestBody and not requestBody:isDead() and threats(requestBody) > 0 then
            request = lastRequest.npc
        end
        local bestBody = request:getWorldObject()
        local sent = 0
        for _, ally in ipairs(deps.roster(player)) do
            if sent >= 2 then break end
            if ally ~= best and not assist[ally] and tellNpcStay then
                local body = ally:getWorldObject()
                if body and not body:isDead() and not body:hasTimedActions() and not body:isAttacking() then
                    local dx, dy = body:getX() - bestBody:getX(), body:getY() - bestBody:getY()
                    if dx * dx + dy * dy <= 400 then
                        local bx, by = bestBody:getX(), bestBody:getY()
                        local dx, dy = player:getX() - bx, player:getY() - by
                        local length = math.sqrt(dx * dx + dy * dy)
                        local coverX, coverY = bx, by
                        if length > 0.1 then
                            coverX = bx + dx / length * 3
                            coverY = by + dy / length * 3
                        end
                        local ok = pcall(tellNpcStay, ally, coverX, coverY, bestBody:getZ())
                        if ok then
                            assist[ally] = {untilAt = now + 20000, target = request}
                            sent = sent + 1
                            if Settings.diagnosticsEnabled() then
                                print("[PersonalKnox][help-cover] npc=" .. tostring(ally:getId())
                                    .. " to=" .. tostring(request:getId()))
                            end
                            deps.say(player, ally, "help_rush")
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
