-- Medical care for the own NPC layer; Knox roster never contains these IsoPlayer bodies.
local OwnNpcMedic = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local INTERVAL_MS, lastCheck = 10000, 0

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

local function findItem(body, fullType)
    local items = body:getInventory():getItems()
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        if item:getFullType() == fullType then return item end
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

local function distanceSq(a, b)
    local dx, dy = a:getX() - b:getX(), a:getY() - b:getY()
    return dx * dx + dy * dy
end

local function otherBodies(patientId, bodies, includeBusyId)
    local result = {}
    for id, body in pairs(bodies) do
        if id ~= patientId and body and not body:isDead()
                and (id == includeBusyId or not body:hasTimedActions()) then
            result[#result + 1] = {id = id, body = body}
        end
    end
    return result
end

local function findBandage(patientId, patient, bodies, onlyMedicId)
    local best, bestItem, bestDistance = nil, nil, math.huge
    for _, other in ipairs(otherBodies(patientId, bodies, onlyMedicId)) do
        local distance = distanceSq(other.body, patient)
        if distance <= 25 and distance < bestDistance then
            local item = cleanBandage(other.body)
            if item then best, bestItem, bestDistance = other.body, item, distance end
        end
    end
    return best, bestItem, bestDistance
end

local function findSplint(patientId, patient, bodies, onlyMedicId)
    local best, bestSplint, bestSheet, bestPlank, bestDistance = nil, nil, nil, nil, math.huge
    for _, other in ipairs(otherBodies(patientId, bodies, onlyMedicId)) do
        local distance = distanceSq(other.body, patient)
        if (not onlyMedicId or other.id == onlyMedicId) and distance <= 36 and distance < bestDistance then
            local splint = findItem(other.body, "Base.Splint")
            local sheet = findItem(other.body, "Base.RippedSheets")
            local plank = findItem(other.body, "Base.Plank")
            if splint or (sheet and plank) then
                best, bestSplint, bestSheet, bestPlank, bestDistance =
                    other.body, splint, sheet, plank, distance
            end
        end
    end
    return best, bestSplint, bestSheet, bestPlank, bestDistance
end

function OwnNpcMedic.tick(onlyId, onlyMedicId, force)
    if isClient() or not Settings.autoMedic() or (not force and Perf.skip()) then return nil end
    if not PersonalKnoxAPI then return nil end
    local api = PersonalKnoxAPI.getInstance()
    if not api then return nil end
    local bodies = {}
    for i = 0, api:ownCount() - 1 do
        local id = api:ownId(i)
        if id ~= "" then
            local ok, body = pcall(function() return api:ownBodyObject(id) end)
            if ok and body then bodies[id] = body end
        end
    end
    for id, patient in pairs(bodies) do
        if (not onlyId or id == onlyId) and not patient:isDead()
                and (onlyId or not patient:hasTimedActions()) then
            local part = bleedingPart(patient)
            if part then
                local medic, bandage, distance = findBandage(id, patient, bodies, onlyMedicId)
                if medic and bandage and ISApplyBandage and ISTimedActionQueue then
                    ISTimedActionQueue.add(ISApplyBandage:new(medic, patient, bandage, part, true))
                    if Settings.diagnosticsEnabled() then
                        print("[PersonalKnox][own-medic] bandage id=" .. tostring(id)
                            .. " distance=" .. tostring(distance))
                    end
                    return "bandage"
                end
            end
        end
    end
    for id, patient in pairs(bodies) do
        if (not onlyId or id == onlyId) and not patient:isDead()
                and (onlyId or not patient:hasTimedActions()) then
            local part = fracturedLeg(patient)
            if part then
                local medic, splint, sheet, plank, distance = findSplint(id, patient, bodies, onlyMedicId)
                if medic and ISSplint and ISTimedActionQueue then
                    local action
                    if splint then
                        action = ISSplint:new(medic, patient, nil, splint, part, true)
                    else
                        action = ISSplint:new(medic, patient, sheet, plank, part, true)
                    end
                    ISTimedActionQueue.add(action)
                    if Settings.diagnosticsEnabled() then
                        print("[PersonalKnox][own-medic] splint id=" .. tostring(id)
                            .. " item=" .. tostring(splint and "Base.Splint" or "field")
                            .. " distance=" .. tostring(distance))
                    end
                    return "splint"
                end
            end
        end
    end
    return nil
end

function OwnNpcMedic.install()
    Events.OnGameStart.Add(function() lastCheck = 0 end)
    Events.OnPlayerUpdate.Add(function()
        local now = getTimestampMs()
        if now - lastCheck < INTERVAL_MS then return end
        lastCheck = now
        local ok, err = pcall(OwnNpcMedic.tick)
        if not ok then print("[PersonalKnox] own-medic error: " .. tostring(err)) end
    end)
end

return OwnNpcMedic
