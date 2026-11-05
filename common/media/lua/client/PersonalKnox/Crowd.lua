-- Read-only diagnostic for non-squad NPCs crowding one spot.
local Crowd = {}
local Settings = require "PersonalKnox/Settings"
local INTERVAL_MS, MAX_CELLS, MAX_OBJECTS = 60000, 169, 256
local lastCheck = 0

local function scan(player, isMember)
    local cell = getCell()
    local px, py, pz = math.floor(player:getX()), math.floor(player:getY()), math.floor(player:getZ())
    local buckets, processed = {}, 0
    for dx = -6, 6 do
        for dy = -6, 6 do
            local square = cell:getGridSquare(px + dx, py + dy, pz)
            if square then
                local objects = square:getMovingObjects()
                for i = 0, objects:size() - 1 do
                    processed = processed + 1
                    local body = objects:get(i)
                    if instanceof(body, "IsoNpcPlayer") and not body:isDead() then
                        local npc = body:getMetaCharacter()
                        if npc and not isMember(player, npc) then
                            local key = math.floor(body:getX() / 2) .. ":" .. math.floor(body:getY() / 2)
                            local bucket = buckets[key] or {count = 0, x = body:getX(), y = body:getY()}
                            bucket.count = bucket.count + 1
                            buckets[key] = bucket
                        end
                    end
                    if processed >= MAX_OBJECTS then return buckets, processed end
                end
            end
        end
    end
    return buckets, processed
end

local function poll(player, isMember)
    if isClient() or not Settings.diagnosticsEnabled() then return end
    local now = getTimestampMs()
    if now - lastCheck < INTERVAL_MS then return end
    lastCheck = now
    local buckets = scan(player, isMember)
    local best
    for _, bucket in pairs(buckets) do
        if not best or bucket.count > best.count then best = bucket end
    end
    if best and best.count >= 4 then
        print("[PersonalKnox][crowd] count=" .. tostring(best.count)
            .. " x=" .. string.format("%.1f", best.x) .. " y=" .. string.format("%.1f", best.y)
            .. " player=" .. string.format("%.1f", player:getX()) .. "," .. string.format("%.1f", player:getY()))
    end
end

function Crowd.install(deps)
    Events.OnGameStart.Add(function() lastCheck = 0 end)
    Events.OnPlayerUpdate.Add(function()
        local player = getSpecificPlayer(0)
        if not player or player:isDead() then return end
        local ok, err = pcall(poll, player, deps.isMember)
        if not ok then print("[PersonalKnox] Crowd poll error: " .. tostring(err)) end
    end)
end

return Crowd
