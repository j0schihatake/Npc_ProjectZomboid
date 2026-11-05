-- A surrounded companion shouts for help, gated by the help-frequency setting.
local HelpCall = {}
local Help = require "PersonalKnox/Help"
local Settings = require "PersonalKnox/Settings"
local INTERVAL_MS, RADIUS, MAX_NPCS, MAX_OBJECTS = 5000, 4, 4, 128
local lastCheck = 0

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
    for _, npc in ipairs(deps.roster(player)) do
        if checked >= MAX_NPCS then break end
        local body = npc:getWorldObject()
        if body and not body:isDead() then
            local dx, dy = body:getX() - player:getX(), body:getY() - player:getY()
            if dx * dx + dy * dy <= 900 then
                checked = checked + 1
                local count = threats(body)
                if count >= 3 and count > bestCount then best, bestCount = npc, count end
            end
        end
    end
    if best and Help.allowed(now) then
        deps.say(player, best, "react_horde")
        if Settings.diagnosticsEnabled() then
            print("[PersonalKnox][help-call] npc=" .. tostring(best:getId()) .. " zombies=" .. tostring(bestCount))
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
