-- Wounded companions bandage themselves with a clean bandage from their inventory.
local NpcCare = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local INTERVAL_MS, MAX_NPCS = 10000, 2
local lastCheck, deps = 0, nil

local function cleanBandage(body)
    local items = body:getInventory():getItems()
    local dirty = nil
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        if item:isCanBandage() then
            local kind = (item:getType() or ""):lower()
            if not kind:find("dirty", 1, true) then return item end
            dirty = dirty or item
        end
    end
    return dirty
end

local function bleedingPart(body)
    local parts = body:getBodyDamage():getBodyParts()
    for i = 0, parts:size() - 1 do
        local part = parts:get(i)
        if part:bleeding() and not part:bandaged() then return part end
    end
    return nil
end

local function comradeBandage(body, player)
    local best, bestItem, bestDistance = nil, nil, math.huge
    for _, other in ipairs(deps.roster(player)) do
        local otherBody = other:getWorldObject()
        if otherBody and otherBody ~= body and not otherBody:isDead() and not otherBody:hasTimedActions() then
            local dx, dy = otherBody:getX() - body:getX(), otherBody:getY() - body:getY()
            local distance = dx * dx + dy * dy
            if distance <= 36 and distance < bestDistance then
                local item = cleanBandage(otherBody)
                if item then best, bestItem, bestDistance = otherBody, item, distance end
            end
        end
    end
    return best, bestItem, bestDistance
end

local ISNpcRipClothing = nil
if ISBaseTimedAction then
    ISNpcRipClothing = ISBaseTimedAction:derive("ISNpcRipClothing")
    function ISNpcRipClothing:isValid()
        return self.item:getContainer() == self.container
    end
    function ISNpcRipClothing:start()
        self:setActionAnim("Bandage")
    end
    function ISNpcRipClothing:perform()
        self.container:Remove(self.item)
        self.character:getInventory():AddItem("Base.RippedSheets")
        ISBaseTimedAction.perform(self)
    end
    function ISNpcRipClothing:new(character, item, container)
        local o = ISBaseTimedAction.new(self, character)
        o.character = character
        o.item = item
        o.container = container
        o.maxTime = 50
        return o
    end
end

local function corpseCloth(body)
    if not ISNpcRipClothing then return nil end
    local cell = getCell()
    local px, py, pz = math.floor(body:getX()), math.floor(body:getY()), math.floor(body:getZ())
    local best, bestInv, bestDistance = nil, nil, math.huge
    for dx = -6, 6 do
        for dy = -6, 6 do
            local square = cell:getGridSquare(px + dx, py + dy, pz)
            if square then
                local objects = square:getMovingObjects()
                for i = 0, objects:size() - 1 do
                    local object = objects:get(i)
                    if instanceof(object, "IsoZombie") and object:isDead() and object.getInventory then
                        local inv = object:getInventory()
                        if inv then
                            local items = inv:getItems()
                            for j = 0, items:size() - 1 do
                                local item = items:get(j)
                                if item:IsClothing() then
                                    local distance = dx * dx + dy * dy
                                    if distance < bestDistance then
                                        best, bestInv, bestDistance = item, inv, distance
                                    end
                                end
                            end
                        end
                    end
                end
            end
        end
    end
    return nil
end

local function searchBandage(body)
    local cell = getCell()
    local px, py, pz = math.floor(body:getX()), math.floor(body:getY()), math.floor(body:getZ())
    local processed = 0
    local best, bestContainer, bestDistance = nil, nil, math.huge
    for dx = -6, 6 do
        for dy = -6, 6 do
            local square = cell:getGridSquare(px + dx, py + dy, pz)
            if square then
                local objects = square:getObjects()
                for i = 0, objects:size() - 1 do
                    processed = processed + 1
                    local object = objects:get(i)
                    local container = object.getContainer and object:getContainer()
                    if container then
                        local items = container:getItems()
                        for j = 0, items:size() - 1 do
                            local item = items:get(j)
                            if item:isCanBandage() then
                                local kind = (item:getType() or ""):lower()
                                if not kind:find("dirty", 1, true) then
                                    local distance = dx * dx + dy * dy
                                    if distance < bestDistance then
                                        best, bestContainer, bestDistance = item, container, distance
                                    end
                                end
                            end
                        end
                    end
                    if processed >= 200 then return best, bestContainer, bestDistance end
                end
            end
        end
    end
    return nil
end

local function poll(player)
    if isClient() or not deps or Perf.skip() or not Settings.autoMedic() then return end
    local now = getTimestampMs()
    if now - lastCheck < INTERVAL_MS then return end
    lastCheck = now
    local checked = 0
    for _, npc in ipairs(deps.roster(player)) do
        if checked >= MAX_NPCS then break end
        local body = npc:getWorldObject()
        if body and not body:isDead() and not body:hasTimedActions() then
            checked = checked + 1
            local part = bleedingPart(body)
            local bandage = part and cleanBandage(body)
            if part and not bandage and Settings.npcSearchContainers() then
                local friend, friendItem, friendDistance = comradeBandage(body, player)
                local found, container, containerDistance = searchBandage(body)
                local cloth, corpseInv, corpseDistance = corpseCloth(body)
                local bestDistance = math.min(friendDistance or math.huge,
                    containerDistance or math.huge, corpseDistance or math.huge)
                if friend and friendDistance == bestDistance and ISInventoryTransferAction and ISTimedActionQueue then
                    ISTimedActionQueue.add(ISInventoryTransferAction:new(body, friendItem,
                        friend:getInventory(), body:getInventory()))
                    if Settings.diagnosticsEnabled() then
                        print("[PersonalKnox][npc-care] ask distance=" .. tostring(friendDistance))
                    end
                elseif found and container and containerDistance == bestDistance
                        and ISInventoryTransferAction and ISTimedActionQueue then
                    ISTimedActionQueue.add(ISInventoryTransferAction:new(body, found, container, body:getInventory()))
                    if Settings.diagnosticsEnabled() then
                        print("[PersonalKnox][npc-care] take=" .. tostring(found:getFullType())
                            .. " distance=" .. tostring(containerDistance))
                    end
                elseif cloth and corpseInv and corpseDistance == bestDistance and ISTimedActionQueue then
                    ISTimedActionQueue.add(ISNpcRipClothing:new(body, cloth, corpseInv))
                    if Settings.diagnosticsEnabled() then
                        print("[PersonalKnox][npc-care] rip=" .. tostring(cloth:getFullType())
                            .. " distance=" .. tostring(corpseDistance))
                    end
                end
            end
            if part and bandage and ISApplyBandage and ISTimedActionQueue then
                ISTimedActionQueue.add(ISApplyBandage:new(body, body, bandage, part, true))
                if Settings.diagnosticsEnabled() then
                    print("[PersonalKnox][npc-care] bandage npc=" .. tostring(body))
                end
            end
        end
    end
end

function NpcCare.install(adapters)
    deps = adapters
    Events.OnGameStart.Add(function() lastCheck = 0 end)
    Events.OnPlayerUpdate.Add(function()
        local player = getSpecificPlayer(0)
        if not player or player:isDead() then return end
        local ok, err = pcall(poll, player)
        if not ok then print("[PersonalKnox] NPC-care error: " .. tostring(err)) end
    end)
end

return NpcCare
