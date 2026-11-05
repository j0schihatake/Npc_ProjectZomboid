-- Prototype adapter: own agents get a real IsoPlayer body. No Knox classes.
local OwnNpcBodies = {}
local Perf = require "PersonalKnox/Perf"

local bodies, last = {}, 0
local INTERVAL_MS = 5000

local function spawnBody(id, x, y, z)
    local desc = SurvivorFactory.CreateSurvivor()
    local body = IsoPlayer.new(getWorld():getCell(), desc, x, y, z)
    pcall(function() body:setSceneCulled(false) end)
    local okNpc = pcall(function() body:setNpc(true) end)
    if not okNpc then pcall(function() body:setNPC(true) end) end
    pcall(function() body:setBlockMovement(false) end)
    pcall(function() body:setUsername("PK-" .. id) end)
    bodies[id] = body
    return body
end

local function removeBody(body)
    pcall(function() body:removeFromWorld() end)
    pcall(function() body:delete() end)
end

local function tick()
    if isClient() or not PersonalKnoxAPI then return end
    local player = getSpecificPlayer(0)
    if not player then return end
    local now = getTimestampMs()
    if now - last < INTERVAL_MS then return end
    last = now
    if Perf.skip() then return end
    local api = PersonalKnoxAPI.getInstance()
    local seen = {}
    for i = 0, api:ownCount() - 1 do
        local id = api:ownId(i)
        if id ~= "" then
            seen[id] = true
            local x, y, z = api:ownX(i), api:ownY(i), api:ownZ(i)
            local body = bodies[id]
            if not body then
                local ok, created = pcall(spawnBody, id, x, y, z)
                if ok then body = created else print("[PersonalKnox] Own body error: " .. tostring(created)) end
            end
            if body then
                pcall(function() body:setX(x); body:setY(y); body:setZ(z) end)
            end
        end
    end
    for id, body in pairs(bodies) do
        if not seen[id] then
            removeBody(body)
            bodies[id] = nil
        end
    end
end

function OwnNpcBodies.install()
    Events.OnPlayerUpdate.Add(function()
        local ok, err = pcall(tick)
        if not ok then print("[PersonalKnox] Own body poll error: " .. tostring(err)) end
    end)
end

return OwnNpcBodies
