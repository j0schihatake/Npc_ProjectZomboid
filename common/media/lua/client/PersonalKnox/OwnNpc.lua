-- Thin game-event bridge; own NPC state and bodies live in the Java core.
local OwnNpc = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local Util = require "PersonalKnox/Util"

local deps
local INTERVAL_MS, LOG_MS = 2000, 60000
local last, lastLog, logged, lastSave = 0, 0, false, 0
local pendingEquipment = nil
local lastCorpseClean = 0
local lastBaseProduce = 0
local lastBaseDaily = 0
local pendingBase = nil

local function saveState()
    if isClient() or not PersonalKnoxAPI then return end
    local player=getSpecificPlayer(0)
    if not player then return end
    local api=PersonalKnoxAPI.getInstance()
    local data=player:getModData()
    local agents,population,targetZ=api:ownSnapshot(),api:ownPopulationSnapshot(),api:ownTargetZSnapshot()
    data.PersonalKnoxOwnNpc=agents
    data.PersonalKnoxOwnPopulation=population
    data.PersonalKnoxTargetZ=targetZ
    data.PersonalKnoxEquipment=api:ownEquipmentSnapshot()
    data.PersonalKnoxBase=api:ownBaseSnapshot()
end

local function tick()
    if isClient() or not PersonalKnoxAPI then return end
    local player = getSpecificPlayer(0)
    if not player or player:isDead() then return end
    local now = getTimestampMs()
    local interval = INTERVAL_MS
    local okFps, fps = pcall(function() return getAverageFPS() end)
    if okFps and type(fps) == "number" and fps > 0 and fps < 30 then interval = 5000 end
    if now - last < interval then return end
    last = now
    if Perf.skip() then return end
    local api = PersonalKnoxAPI.getInstance()
    local members = api:knoxGroupMembers(player)
    if members == 0 then
        for _, npc in ipairs(deps.roster(player)) do
            if npc:getWorldObject() and not npc:getWorldObject():isDead() then members = members + 1 end
        end
    end
    local ownActive = api:ownActive()
    if ownActive > members then members = ownActive end
    local px, py, pz = player:getX(), player:getY(), player:getZ()
    api:ownGroup("player-squad", "own", members, px, py)
    api:ownSync(px, py, 80, members)
    api:ownFollowTarget3(px, py, pz)
    api:ownTick(0.01, 32)
    local active = api:ownActive()
    local bodies = api:ownBodiesSync()
    if now - lastBaseDaily >= 300000 then
        lastBaseDaily = now
        print("[PersonalKnox][base] daily " .. tostring(api:ownBaseDaily(api:ownActive())))
    end
    if now - lastBaseProduce >= 60000 then
        lastBaseProduce = now
        if api:ownHasProfession("COOK") then
            print("[PersonalKnox][base] cook " .. tostring(api:ownBaseCook()))
        end
        for _, profession in ipairs({"WATER", "FARMER"}) do
            if api:ownHasProfession(profession) then
                local stock = api:ownBaseProduce(profession)
                print("[PersonalKnox][base] " .. profession .. " " .. tostring(stock))
            end
        end
        if api:ownHasProfession("FARMER") then
            print("[PersonalKnox][base] farm " .. tostring(api:ownFarmTick(true)))
        end
        if api:ownHasProfession("BUILD") then
            print("[PersonalKnox][base] build " .. tostring(api:ownBlueprintTick(1)))
        end
        if api:ownHasProfession("CRAFT") then
            print("[PersonalKnox][base] ammo " .. tostring(api:ownBaseCraftAmmo()))
        end
        if api:ownHasProfession("FISH") then
            print("[PersonalKnox][base] fish " .. tostring(api:ownBaseFish()))
        end
        if api:ownHasProfession("HUNT") then
            print("[PersonalKnox][base] hunt " .. tostring(api:ownBaseHunt()))
        end
        if api:ownHasProfession("LAUNDRY") then
            print("[PersonalKnox][base] laundry " .. tostring(api:ownBaseLaundry()))
        end
    end
    if now - lastCorpseClean >= 30000 then
        lastCorpseClean = now
        if api:ownHasProfession("CORPSE") then
            local cleaned = api:ownCleanCorpses(15)
            if cleaned and cleaned > 0 then print("[PersonalKnox][base] corpses=" .. tostring(cleaned)) end
        end
    end
    if pendingBase and api:ownActive() > 0 then
        pcall(function() api:ownRestoreBase(pendingBase) end)
        pendingBase = nil
    end
    if pendingEquipment and api:ownActive() > 0 then
        pcall(function() api:ownRestoreEquipment(pendingEquipment) end)
        pendingEquipment = nil
    end
    if now - lastSave >= 60000 then
        lastSave = now
        saveState()
    end
    if not logged then
        logged = true
        print("[PersonalKnox][knox-roster] " .. tostring(api:knoxMemberSnapshot(player)))
        print("[PersonalKnox][own-npc] members=" .. tostring(members) .. " active=" .. tostring(active) .. " bodies=" .. tostring(bodies) .. " " .. tostring(api:knoxStatus()))
    elseif Settings.diagnosticsEnabled() and now - lastLog >= LOG_MS then
        lastLog = now
        print("[PersonalKnox][own-npc] members=" .. tostring(members) .. " active=" .. tostring(active) .. " bodies=" .. tostring(bodies) .. " " .. tostring(api:knoxStatus()))
    end
end

local function ensurePlayerClothes(player)
    if not player then return end
    local worn = player:getWornItems()
    if worn and worn:size() >= 3 then return end
    local inventory = player:getInventory()
    local slots = ItemBodyLocation
    local items = {
        {"Base.Tshirt_WhiteLongSleeve", slots.TSHIRT},
        {"Base.Trousers", slots.PANTS},
        {"Base.Shoes_Black", slots.SHOES},
    }
    for _, entry in ipairs(items) do
        local item = inventory:AddItem(entry[1])
        if item then player:setWornItem(entry[2], item) end
    end
    print("[PersonalKnox] player clothes restored")
end

function OwnNpc.install(adapters)
    deps = adapters
    Events.OnGameStart.Add(function()
        local player = getSpecificPlayer(0)
        if not player or not PersonalKnoxAPI then return end
        local data = player:getModData() or {}
        pendingEquipment = data.PersonalKnoxEquipment
        pendingBase = data.PersonalKnoxBase
        local ok, result = pcall(function()
            return PersonalKnoxAPI.getInstance():ownBootstrap(
                player, Settings.companionCount(),
                data.PersonalKnoxOwnNpc or "",
                data.PersonalKnoxTargetZ or "",
                data.PersonalKnoxOwnPopulation or "",
                Settings.matchPlayerClothes())
        end)
        if ok then
            print("[PersonalKnox][own-npc] bootstrap " .. tostring(result))
        else
            print("[PersonalKnox] Own-NPC bootstrap error: " .. tostring(result))
        end
        pcall(ensurePlayerClothes, player)
    end)
    Events.OnSave.Add(function()
        local ok,err=pcall(saveState)
        if not ok then print("[PersonalKnox] Own-NPC save error: "..tostring(err)) end
    end)
    Events.OnPlayerUpdate.Add(function()
        local ok, err = pcall(tick)
        if not ok then print("[PersonalKnox] Own-NPC poll error: " .. tostring(err)) end
    end)
end

return OwnNpc
