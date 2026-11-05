-- Prototype: a small threat is discussed; leader may answer, otherwise NPCs agree.
local Negotiation = {}
local Settings = require "PersonalKnox/Settings"
local INTERVAL_MS, ANSWER_MS, COOLDOWN_MS = 2000, 8000, 120000
local lastCheck, lastStart = 0, 0
local pending, deps = nil, nil

local function visibleThreats(player)
    local cell = getCell()
    local px, py, pz = math.floor(player:getX()), math.floor(player:getY()), math.floor(player:getZ())
    local count, nearest = 0, nil
    for dx = -6, 6 do
        for dy = -6, 6 do
            local square = cell:getGridSquare(px + dx, py + dy, pz)
            if square then
                local objects = square:getMovingObjects()
                for i = 0, objects:size() - 1 do
                    local object = objects:get(i)
                    if instanceof(object, "IsoZombie") and not object:isDead() and player:CanSee(object) then
                        count = count + 1
                        if not nearest then nearest = object end
                    end
                end
            end
        end
    end
    return count, nearest
end

local function collectVotes(player)
    local attack, hold, retreat = 0, 0, 0
    local speaker, members = nil, 0
    for _, npc in ipairs(deps.roster(player)) do
        if members >= 4 then break end
        local body = npc:getWorldObject()
        if body and not body:isDead() then
            members = members + 1
            speaker = speaker or npc
            local bravery = 50
            pcall(function() bravery = npc:getPersonality():getBravery() end)
            local armed = body:getPrimaryHandItem() ~= nil
            local health = 100
            pcall(function() health = body:getBodyDamage():getHealth() end)
            attack = attack + 1 + bravery / 100 + (armed and 1 or 0)
            hold = hold + 1 + (health < 80 and 0.5 or 1)
            retreat = retreat + 1 + (health < 60 and 2 or 0)
        end
    end
    return speaker, attack, hold, retreat
end

local function resolve(player)
    if not pending then return end
    local api = PersonalKnoxAPI and PersonalKnoxAPI.getInstance()
    if not api then pending = nil; return end
    local choice = api:groupChoice(pending.attack, pending.hold, pending.retreat, pending.leader or 0)
    deps.say(player, pending.speaker, "squad_" .. choice)
    if Settings.diagnosticsEnabled() then
        print("[PersonalKnox][negotiation] choice=" .. tostring(choice)
            .. " leader=" .. tostring(pending.leader or 0)
            .. " attack=" .. string.format("%.1f", pending.attack)
            .. " hold=" .. string.format("%.1f", pending.hold)
            .. " retreat=" .. string.format("%.1f", pending.retreat))
    end
    pending = nil
end

local function poll(player)
    if isClient() or not PersonalKnoxAPI or not Settings.speechEnabled() then return end
    local now = getTimestampMs()
    if now - lastCheck < INTERVAL_MS then return end
    lastCheck = now
    if pending then
        if pending.leader or now >= pending.untilAt then resolve(player) end
        return
    end
    if now - lastStart < COOLDOWN_MS then return end
    if player:isTargetedByZombie() then return end
    local count, nearest = visibleThreats(player)
    if count < 1 or count > 2 then return end
    local speaker, attack, hold, retreat = collectVotes(player)
    if not speaker then return end
    lastStart = now
    deps.say(player, speaker, "squad_question")
    pending = {speaker = speaker, untilAt = now + ANSWER_MS,
        attack = attack, hold = hold, retreat = retreat, leader = nil}
    if Settings.diagnosticsEnabled() then
        print("[PersonalKnox][negotiation] start zombies=" .. tostring(count))
    end
end

function Negotiation.answer(player, choice)
    if pending then pending.leader = choice end
end

function Negotiation.addOptions(menu, player)
    if not pending then return end
    local root = menu:addOption(getText("ContextMenu_PersonalKnox_AnswerSquad"), nil, nil)
    local sub = menu:getNew(menu)
    menu:addSubMenu(root, sub)
    sub:addOption(getText("ContextMenu_PersonalKnox_AnswerAttack"), player, Negotiation.answer, 1)
    sub:addOption(getText("ContextMenu_PersonalKnox_AnswerHold"), player, Negotiation.answer, 2)
    sub:addOption(getText("ContextMenu_PersonalKnox_AnswerRetreat"), player, Negotiation.answer, 3)
end

function Negotiation.install(adapters)
    deps = adapters
    Events.OnGameStart.Add(function() lastCheck, lastStart, pending = 0, 0, nil end)
    Events.OnPlayerUpdate.Add(function()
        local player = getSpecificPlayer(0)
        if not player or player:isDead() then return end
        local ok, err = pcall(poll, player)
        if not ok then print("[PersonalKnox] Negotiation error: " .. tostring(err)) end
    end)
end

return Negotiation
