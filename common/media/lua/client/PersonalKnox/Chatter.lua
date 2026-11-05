-- Companions sometimes start a short conversation on their own, not only reply.
local Chatter = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local Util = require "PersonalKnox/Util"
local CHECK_MS = 30000
local function gap()
    local rate = Settings.chatterFrequency()
    if rate == 1 then return nil end
    if rate == 2 then return 240000, 420000 end
    if rate == 4 then return 45000, 90000 end
    return 90000, 180000
end
local lastCheck, nextAt, deps = 0, 0, nil

local function poll(player)
    if not deps or not Settings.speechEnabled() or isClient() or Perf.lowFps() then return end
    local minGap, maxGap = gap()
    if not minGap then return end
    local now = getTimestampMs()
    if now - lastCheck < CHECK_MS then return end
    lastCheck = now
    if now < nextAt then return end
    if player:isTargetedByZombie() or player:isAiming() then return end
    local speaker, best = nil, 1e9
    for _, npc in ipairs(deps.roster(player)) do
        local body = npc:getWorldObject()
        if body and not body:isDead() then
            local dx, dy = body:getX() - player:getX(), body:getY() - player:getY()
            local d = dx * dx + dy * dy
            if d < 100 and d < best then speaker, best = npc, d end
        end
    end
    if not speaker then return end
    deps.say(player, speaker, "smalltalk")
    nextAt = now + minGap + math.random() * (maxGap - minGap)
    if Settings.diagnosticsEnabled() then
        print("[PersonalKnox][chatter] npc=" .. tostring(Util.id(speaker)))
    end
end

function Chatter.install(adapters)
    deps = adapters
    Events.OnGameStart.Add(function() lastCheck, nextAt = 0, 0 end)
    Events.OnPlayerUpdate.Add(function()
        local player = getSpecificPlayer(0)
        if not player or player:isDead() then return end
        local ok, err = pcall(poll, player)
        if not ok then print("[PersonalKnox] Chatter error: " .. tostring(err)) end
    end)
end

return Chatter
