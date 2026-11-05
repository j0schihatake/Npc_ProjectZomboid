-- Companions occasionally point out valuable tools found in containers.
local NpcFinds = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local INTERVAL_MS, COOLDOWN_MS, MAX_NPCS = 30000, 60000, 2
local VALUABLE = {
    "sledgehammer", "axe", "hammer", "nails", "crowbar", "pipe", "saw", "wrench", "screwdriver",
    "rifle", "pistol", "shotgun", "revolver", "carbine", "smg", "gun", "ammo", "bullet",
    "vest", "armor", "armour", "helmet", "kevlar", "plate",
    "medkit", "firstaid", "bandage", "antibiotics", "pills", "syringe"
}
local lastCheck, lastAnnounce, deps = 0, 0, nil

local function valuable(item)
    local kind = (item:getType() or ""):lower()
    for _, word in ipairs(VALUABLE) do
        if kind:find(word, 1, true) then return word end
    end
    return nil
end

local function find(body)
    local cell = getCell()
    local px, py, pz = math.floor(body:getX()), math.floor(body:getY()), math.floor(body:getZ())
    local processed = 0
    for dx = -6, 6 do
        for dy = -6, 6 do
            local square = cell:getGridSquare(px + dx, py + dy, pz)
            if square then
                local objects = square:getObjects()
                for i = 0, objects:size() - 1 do
                    local object = objects:get(i)
                    processed = processed + 1
                    local container = object.getContainer and object:getContainer()
                    if container then
                        local items = container:getItems()
                        for j = 0, items:size() - 1 do
                            local item = items:get(j)
                            local kind = valuable(item)
                            if kind then return item, kind, dx * dx + dy * dy end
                        end
                    end
                    if processed >= 200 then return nil end
                end
            end
        end
    end
    return nil
end

local function poll(player)
    if isClient() or not deps or Perf.skip() or not Settings.npcFinds() then return end
    local now = getTimestampMs()
    if now - lastCheck < INTERVAL_MS then return end
    lastCheck = now
    if now - lastAnnounce < COOLDOWN_MS then return end
    local checked = 0
    for _, npc in ipairs(deps.roster(player)) do
        if checked >= MAX_NPCS then break end
        local body = npc:getWorldObject()
        if body and not body:isDead() and not body:hasTimedActions() then
            checked = checked + 1
            local dx, dy = body:getX() - player:getX(), body:getY() - player:getY()
            if dx * dx + dy * dy <= 100 then
                local item, kind, distance = find(body)
                if item then
                    lastAnnounce = now
                    deps.say(player, npc, "find_valuable")
                    pcall(function() body:playAnim("Point") end)
                    if Settings.diagnosticsEnabled() then
                        print("[PersonalKnox][npc-find] " .. kind .. "=" .. tostring(item:getType())
                            .. " distance=" .. tostring(distance))
                    end
                    return
                end
            end
        end
    end
end

function NpcFinds.install(adapters)
    deps = adapters
    Events.OnGameStart.Add(function() lastCheck, lastAnnounce = 0, 0 end)
    Events.OnPlayerUpdate.Add(function()
        local player = getSpecificPlayer(0)
        if not player or player:isDead() then return end
        local ok, err = pcall(poll, player)
        if not ok then print("[PersonalKnox] NPC-finds error: " .. tostring(err)) end
    end)
end

return NpcFinds
