-- Bounded world sampling. Java decides which event may produce a line.
local Reactions = { cursor = 0 }
local adapters
local RADIUS, MAX_OBJECTS, MAX_THREATS, MAX_NPCS = 6, 256, 24, 8

local function need(body, stat)
    local ok, value = pcall(function() return body:getStats():get(stat) end)
    if ok and type(value) == "number" then return value end
    return -1 -- Unknown is not evidence of hunger; keep other reactions working.
end

local function enabled(player)
    local data = player:getModData()
    data.PersonalKnox = data.PersonalKnox or {}
    return data.PersonalKnox.worldReactions ~= false
end

function Reactions.addOption(menu, player)
    menu:addOption(enabled(player) and getText("ContextMenu_PersonalKnox_ReactionsOff") or getText("ContextMenu_PersonalKnox_ReactionsOn"), player, function(p)
        p:getModData().PersonalKnox.worldReactions = not enabled(p)
        if PersonalKnoxAPI then PersonalKnoxAPI.getInstance():resetWorldReactions() end
    end)
end

local function sample(player)
    local cell, members, threats, seen = getCell(), {}, {}, {}
    local processed, corpses = 0, false
    local px, py, pz = math.floor(player:getX()), math.floor(player:getY()), math.floor(player:getZ())
    local function add(body)
        if #members >= MAX_NPCS or body:isDead() then return end
        local npc = body:getMetaCharacter()
        if npc and not seen[npc] and adapters.isMember(player, npc) then
            seen[npc] = true
            members[#members + 1] = npc
        end
    end
    -- Visit the closest squares first; a dense horde cannot cause an unbounded scan.
    for radius = 0, RADIUS do
        for dx = -radius, radius do
            for dy = -radius, radius do
                if radius == 0 or math.abs(dx) == radius or math.abs(dy) == radius then
                    local square = cell:getGridSquare(px + dx, py + dy, pz)
                    if square then
                        if not corpses and square:getDeadBody() ~= nil then corpses = true end
                        local objects = square:getMovingObjects()
                        for i = 0, objects:size() - 1 do
                            processed = processed + 1
                            local body = objects:get(i)
                            if instanceof(body, "IsoNpcPlayer") then add(body)
                            elseif #threats < MAX_THREATS and instanceof(body, "IsoZombie") and not body:isDead() then
                                threats[#threats + 1] = body
                            end
                            if processed >= MAX_OBJECTS then return members, threats, corpses end
                        end
                    end
                end
            end
        end
    end
    return members, threats, corpses
end

function Reactions.poll()
    if isClient() or not PersonalKnoxAPI or not classFilesInstalled then return end
    local player = getSpecificPlayer(0)
    if not player or player:isDead() or not enabled(player) then return end
    local members, threats, corpses = sample(player)
    if #members == 0 then return end
    local now = getTimestampMs()
    local time = getGameTime():getTimeOfDay()
    local rain = getClimateManager():getRainIntensity() > 0.1
    local playerHealth = player:getBodyDamage():getHealth()
    local java = PersonalKnoxAPI.getInstance()
    -- At most four nearby companions per poll, in rotating order.
    for offset = 1, math.min(4, #members) do
        local npc = members[((Reactions.cursor + offset - 1) % #members) + 1]
        local body = npc:getWorldObject()
        if body and not body:isDead() and adapters.isMember(player, npc) then
            local visible = 0
            for _, zombie in ipairs(threats) do
                local dx, dy = body:getX() - zombie:getX(), body:getY() - zombie:getY()
                if dx * dx + dy * dy <= 64 and body:CanSee(zombie) then visible = visible + 1 end
            end
            local outside = body:getCurrentSquare() and body:getCurrentSquare():isOutside() or false
            local damage = body:getBodyDamage()
            local topic = java:worldReactionWithNeeds(tostring(npc:getId()), now, damage:getHealth(), playerHealth,
                damage:getNumPartsBleeding() > 0, visible, rain and outside,
                outside and (time >= 21 or time < 5), corpses,
                need(body, CharacterStat and CharacterStat.HUNGER),
                need(body, CharacterStat and CharacterStat.THIRST),
                need(body, CharacterStat and CharacterStat.FATIGUE))
            if topic ~= "" then adapters.say(player, npc, topic) end
        end
    end
    Reactions.cursor = (Reactions.cursor + 4) % #members
end

function Reactions.install(deps)
    adapters = deps
    Events.OnGameStart.Add(function()
        Reactions.cursor = 0
        if PersonalKnoxAPI then PersonalKnoxAPI.getInstance():resetWorldReactions() end
    end)
    Events.EveryOneMinute.Add(function()
        local ok, err = pcall(Reactions.poll)
        if not ok then print("[PersonalKnox] World reaction error: " .. tostring(err)) end
    end)
end
return Reactions
