-- Hungry/thirsty companions eat and drink from their own inventory.
local NpcEat = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local Items = require "PersonalKnox/SupplyItems"
local Util = require "PersonalKnox/Util"
local INTERVAL_MS, MAX_NPCS, MAX_ITEMS = 10000, 2, 128
local lastCheck, npcCursor, cursors, deps = 0, 0, {}, nil

local function findItems(body, needFood, needWater, key, limit)
    local items = body:getInventory():getItems()
    local count=items:size()
    if count==0 then return nil,nil end
    local offset=cursors[key] or 0
    local java=PersonalKnoxAPI.getInstance()
    local food,water
    for i = 0, math.min(count,limit) - 1 do
        local index=(offset+i)%count
        cursors[key]=(index+1)%count
        local item=items:get(index)
        if item then
            if needFood and not food and Items.food(body,item,java) then food=item end
            if needWater and not water and Items.water(item,java) then water=item end
            if (food or not needFood) and (water or not needWater) then break end
        end
    end
    return food,water
end

local function safe(body)
    local ok, calm=pcall(function()
        return body:hasNoDangerousTargets() and not body:hasEnemyNpcTargets() and not body:isInActiveCombat()
    end)
    return ok and calm==true
end

local function eat(body, item, drink)
    if drink and ISDrinkFluidAction and ISTimedActionQueue then
        ISTimedActionQueue.add(ISDrinkFluidAction:new(body, item, 1))
        return true
    elseif not drink and ISEatFoodAction and ISTimedActionQueue then
        ISTimedActionQueue.add(ISEatFoodAction:new(body, item, 1))
        return true
    end
    return false
end

local function poll(player)
    if isClient() or not deps then return end
    local now = getTimestampMs()
    if now - lastCheck < INTERVAL_MS then return end
    lastCheck = now
    -- Essential needs continue at low FPS with smaller budgets, not a total shutdown.
    local constrained=Perf.skip()
    local limit=constrained and 32 or MAX_ITEMS
    local npcLimit=constrained and 1 or MAX_NPCS
    local roster=deps.roster(player)
    local count=#roster
    if count==0 then return end
    local start=npcCursor
    npcCursor=(npcCursor+npcLimit)%count
    for i=0,math.min(count,npcLimit)-1 do
        local npc=roster[(start+i)%count+1]
        local body = npc:getWorldObject()
        if body and not body:isDead() and not body:hasTimedActions() and safe(body) then
            local hunger = body:getStats():get(CharacterStat.HUNGER)
            local thirst = body:getStats():get(CharacterStat.THIRST)
            local needFood=hunger>=0.60 and Settings.autoEat()
            local needWater=thirst>=0.60 and Settings.autoDrink()
            if needFood or needWater then
                local food,water=findItems(body,needFood,needWater,Util.id(npc),limit)
                if food and eat(body, food, false) then
                    if Settings.diagnosticsEnabled() then print("[PersonalKnox][npc-eat] food npc=" .. tostring(body)) end
                elseif water and eat(body, water, true) then
                    if Settings.diagnosticsEnabled() then print("[PersonalKnox][npc-eat] water npc=" .. tostring(body)) end
                end
            end
        end
    end
end

function NpcEat.install(adapters)
    deps = adapters
    Events.OnGameStart.Add(function() lastCheck, npcCursor, cursors = 0, 0, {} end)
    Events.OnPlayerUpdate.Add(function()
        local player = getSpecificPlayer(0)
        if not player or player:isDead() then return end
        local ok, err = pcall(poll, player)
        if not ok then print("[PersonalKnox] NPC-eat error: " .. tostring(err)) end
    end)
end

return NpcEat
