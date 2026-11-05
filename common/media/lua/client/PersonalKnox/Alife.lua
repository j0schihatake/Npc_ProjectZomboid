-- Live A-Life bridge: real squad snapshots in, compact event counts out.
local Alife = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local Util = require "PersonalKnox/Util"

local deps
local INTERVAL_MS, LOG_MS = 5000, 30000
local last, lastLog, firstLogged = 0, 0, false

local function healthOf(body)
    local ok, health = pcall(function() return body:getBodyDamage():getHealth() end)
    if ok and type(health) == "number" then return health end
    return 100
end

local function tick()
    if isClient() or not PersonalKnoxAPI then return end
    local player = getSpecificPlayer(0)
    if not player or player:isDead() then return end
    local now = getTimestampMs()
    if now - last < INTERVAL_MS then return end
    last = now
    if Perf.skip() then return end
    local api = PersonalKnoxAPI.getInstance()
    local px, py, pz = player:getX(), player:getY(), player:getZ()
    local submitted, seen = 0, {}
    for _, npc in ipairs(deps.roster(player)) do
        if submitted >= 16 then break end
        local body = npc:getWorldObject()
        if body and not body:isDead() then
            local id = Util.id(npc)
            if not seen[id] then
                seen[id] = true
                api:alifeSquad(id, "own", 1, healthOf(body), 2, "REGROUP",
                    body:getX(), body:getY(), body:getZ(), px, py)
                submitted = submitted + 1
            end
        end
    end
    local summary = api:alifeTick(now, px, py, pz, 32)
    if not firstLogged then
        firstLogged = true
        print("[PersonalKnox][alife-init] squads=" .. tostring(submitted) .. " events=" .. tostring(summary))
    elseif Settings.diagnosticsEnabled() and now - lastLog >= LOG_MS then
        lastLog = now
        print("[PersonalKnox][alife] squads=" .. tostring(submitted) .. " events=" .. tostring(summary))
    end
end

function Alife.install(adapters)
    deps = adapters
    Events.OnPlayerUpdate.Add(function()
        local ok, err = pcall(tick)
        if not ok then print("[PersonalKnox] A-Life poll error: " .. tostring(err)) end
    end)
end

return Alife
