-- A healthy companion with a bandage treats a bleeding squadmate.
local NpcMedic = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local INTERVAL_MS = 10000
local lastCheck, deps = 0, nil

local function cleanBandage(body)
    local items = body:getInventory():getItems()
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        if item:isCanBandage() then
            local kind = (item:getType() or ""):lower()
            if not kind:find("dirty", 1, true) then return item end
        end
    end
    return nil
end

local function bleedingPart(body)
    local parts = body:getBodyDamage():getBodyParts()
    for i = 0, parts:size() - 1 do
        local part = parts:get(i)
        if part:bleeding() and not part:bandaged() then return part end
    end
    return nil
end

local function fracturedLeg(body)
    local parts = body:getBodyDamage():getBodyParts()
    for i = 0, parts:size() - 1 do
        local part = parts:get(i)
        local kind = part:getType()
        if (kind == BodyPartType.LowerLeg_L or kind == BodyPartType.LowerLeg_R
                or kind == BodyPartType.UpperLeg_L or kind == BodyPartType.UpperLeg_R)
                and part:getFractureTime() > 0 and part:getSplintFactor() == 0 then
            return part
        end
    end
    return nil
end

local function findItem(body, fullType)
    local items = body:getInventory():getItems()
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        if item:getFullType() == fullType then return item end
    end
    return nil
end

local function findSplintMedic(patientNpc, patient, roster)
    local bestMedic, bestSplint, bestSheet, bestPlank, bestDistance = nil, nil, nil, nil, math.huge
    for _, medicNpc in ipairs(roster) do
        if medicNpc ~= patientNpc then
            local medic = medicNpc:getWorldObject()
            if medic and not medic:isDead() and not medic:hasTimedActions() then
                local dx, dy = medic:getX() - patient:getX(), medic:getY() - patient:getY()
                local distance = dx * dx + dy * dy
                if distance <= 36 and distance < bestDistance then
                    local splint = findItem(medic, "Base.Splint")
                    local sheet = findItem(medic, "Base.RippedSheets")
                    local plank = findItem(medic, "Base.Plank")
                    if splint or (sheet and plank) then
                        bestMedic, bestSplint, bestSheet, bestPlank, bestDistance =
                            medic, splint, sheet, plank, distance
                    end
                end
            end
        end
    end
    return bestMedic, bestSplint, bestSheet, bestPlank, bestDistance
end

local function poll(player)
    if isClient() or not deps or Perf.essentialSkip() or not Settings.autoMedic() then return end
    local now = getTimestampMs()
    if now - lastCheck < INTERVAL_MS then return end
    lastCheck = now
    local roster = deps.roster(player)
    if #roster < 2 then return end
    for _, patientNpc in ipairs(roster) do
        local patient = patientNpc:getWorldObject()
        if patient and not patient:isDead() and not patient:hasTimedActions() then
            local part = bleedingPart(patient)
            if part then
                local bestMedic, bestBandage, bestDistance = nil, nil, math.huge
                for _, medicNpc in ipairs(roster) do
                    if medicNpc ~= patientNpc then
                        local medic = medicNpc:getWorldObject()
                        if medic and not medic:isDead() and not medic:hasTimedActions() then
                            local dx, dy = medic:getX() - patient:getX(), medic:getY() - patient:getY()
                            local distance = dx * dx + dy * dy
                            if distance <= 25 and distance < bestDistance then
                                local bandage = cleanBandage(medic)
                                if bandage then
                                    bestMedic, bestBandage, bestDistance = medic, bandage, distance
                                end
                            end
                        end
                    end
                end
                if bestMedic and bestBandage and ISApplyBandage and ISTimedActionQueue then
                    ISTimedActionQueue.add(ISApplyBandage:new(bestMedic, patient, bestBandage, part, true))
                    if Settings.diagnosticsEnabled() then
                        print("[PersonalKnox][npc-medic] bandage patient=" .. tostring(patient)
                            .. " distance=" .. tostring(bestDistance))
                    end
                    return
                end
            end
        end
    end
    -- A fractured leg gets a real splint, including in the field: use a ready
    -- Base.Splint or the vanilla sheet+plank combination consumed by ISSplint.
    for _, patientNpc in ipairs(roster) do
        local patient = patientNpc:getWorldObject()
        if patient and not patient:isDead() and not patient:hasTimedActions() then
            local part = fracturedLeg(patient)
            if part then
                local medic, splint, sheet, plank, distance = findSplintMedic(patientNpc, patient, roster)
                if medic and ISSplint and ISTimedActionQueue then
                    local action
                    if splint then
                        action = ISSplint:new(medic, patient, nil, splint, part, true)
                    else
                        action = ISSplint:new(medic, patient, sheet, plank, part, true)
                    end
                    ISTimedActionQueue.add(action)
                    if Settings.diagnosticsEnabled() then
                        print("[PersonalKnox][npc-medic] splint patient=" .. tostring(patient)
                            .. " item=" .. tostring(splint and "Base.Splint" or "field")
                            .. " distance=" .. tostring(distance))
                    end
                    return
                end
            end
        end
    end
end

function NpcMedic.install(adapters)
    deps = adapters
    Events.OnGameStart.Add(function() lastCheck = 0 end)
    Events.OnPlayerUpdate.Add(function()
        local player = getSpecificPlayer(0)
        if not player or player:isDead() then return end
        local ok, err = pcall(poll, player)
        if not ok then print("[PersonalKnox] NPC-medic error: " .. tostring(err)) end
    end)
end

return NpcMedic
