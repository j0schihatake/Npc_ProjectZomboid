-- Game adapters only: inventories, context menus and dispatch to Knox's own loot command.
local Core = require "PersonalKnox/Core"
local Supply = { enabled = {}, pending = {}, visited = {} }
local deps

local function settings(player)
    local data = player:getModData()
    data.PersonalKnox = data.PersonalKnox or {}
    data.PersonalKnox.confirmPlans = data.PersonalKnox.confirmPlans ~= false
    return data.PersonalKnox
end

local function inventorySnapshot(body)
    local result = {food=0, water=0, medicine=0, weapon=false}
    local function inspect(container, depth)
        if not container or depth > 4 then return end
        local items = container:getItems()
        for i=0,items:size()-1 do
            local item = items:get(i)
            if instanceof(item, "Food") and not item:isRotten() then result.food=result.food+1 end
            if item:isWaterSource() then result.water=result.water+1 end
            if PersonalKnoxAPI.getInstance():isCleanBandage(item:getFullType(),item:isCanBandage(),item:isInfected()) then
                result.medicine=result.medicine+1
            end
            if instanceof(item,"HandWeapon") and not item:isBroken() then result.weapon=true end
            if instanceof(item,"InventoryContainer") then inspect(item:getInventory(),depth+1) end
        end
    end
    inspect(body:getInventory(),0)
    return result
end

local function inDanger(body)
    local zombies = getCell():getZombieList()
    for i=0,zombies:size()-1 do
        local z = zombies:get(i)
        if not z:isDead() and math.abs(z:getZ()-body:getZ()) < 0.5 and
            Core.distanceSquared({x=body:getX(),y=body:getY()},{x=z:getX(),y=z:getY()}) < 144 then return true end
    end
    return false
end

local function valid(player,npc,building)
    if not deps.isMember(player,npc) then return false end
    local body = npc:getWorldObject()
    local square = player:getCurrentSquare()
    if not body or body:isDead() or not square or square:getBuilding() ~= building then return false end
    if math.abs(player:getZ()-body:getZ()) > 0.5 or inDanger(body) or inDanger(player) then return false end
    return Core.distanceSquared({x=body:getX(),y=body:getY()},{x=player:getX(),y=player:getY()}) <= 100
end

local function execute(player,npc)
    local plan = Supply.pending[npc]
    Supply.pending[npc] = nil
    if not plan or getGameTime():getWorldAgeHours() > plan.expires or not valid(player,npc,plan.building) then
        player:Say(getText("ContextMenu_PersonalKnox_PlanChanged"))
        return
    end
    if not tellNpcLoot then player:Say(getText("ContextMenu_PersonalKnox_LootUnavailable")); return end
    -- Knox's public Lua command takes the selected group and category flags.
    -- Restore its UI state even when its command raises an error.
    local keys = {"lootAmmo","lootFirstAid","lootFood","lootGuns","lootMaterials","lootTools","lootWater","lootWeapons"}
    local old, selected = {}, selectedGroupNpcs
    for _,key in ipairs(keys) do old[key]=_G[key]; _G[key]=false end
    selectedGroupNpcs={npc}
    lootFirstAid=true; lootFood=true; lootWater=true
    lootGuns=plan.kind=="weapons"; lootWeapons=plan.kind=="weapons"
    local ok, err = pcall(tellNpcLoot,nil,plan.building)
    selectedGroupNpcs=selected
    for _,key in ipairs(keys) do _G[key]=old[key] end
    if not ok then
        print("[PersonalKnox] Loot command error: "..tostring(err))
        player:Say(getText("ContextMenu_PersonalKnox_LootFailed"))
        return
    end
    Supply.visited[npc]=plan.building
    deps.journal(player,npc,"supply_dispatched",plan.kind)
end

function Supply.propose(player,npc)
    local square=player:getCurrentSquare()
    local building=square and square:getBuilding()
    if not building or not valid(player,npc,building) then
        player:Say(getText("ContextMenu_PersonalKnox_NeedSafeBuilding"))
        return
    end
    local body=npc:getWorldObject()
    local stock=inventorySnapshot(body)
    local overloaded=body:getInventory():getCapacityWeight() >= body:getMaxWeight()*0.8
    local kind=Core.supplyPlan(stock.food,stock.water,stock.medicine,stock.weapon,false,overloaded)
    if kind=="none" then player:Say(getText("ContextMenu_PersonalKnox_StockReady")); return end
    if kind=="wait" then player:Say(getText("ContextMenu_PersonalKnox_InventoryFull")); return end
    Supply.pending[npc]={kind=kind,building=building,expires=getGameTime():getWorldAgeHours()+1}
    deps.say(player,npc,kind=="weapons" and "plan_weapons" or "plan_supplies")
    deps.journal(player,npc,"supply_proposed",kind)
    if not settings(player).confirmPlans then execute(player,npc)
    else player:Say(getText("ContextMenu_PersonalKnox_PlanPending")) end
end

function Supply.addOptions(menu,player,npc)
    menu:addOption(getText("ContextMenu_PersonalKnox_SupplyPlan"),player,Supply.propose,npc)
    menu:addOption(Supply.enabled[npc] and getText("ContextMenu_PersonalKnox_AutoSupplyOff") or getText("ContextMenu_PersonalKnox_AutoSupplyOn"),player,function(p,n)
        Supply.enabled[n]=not Supply.enabled[n]
        p:Say(Supply.enabled[n] and getText("ContextMenu_PersonalKnox_AutoSupplyEnabled") or getText("ContextMenu_PersonalKnox_AutoSupplyDisabled"))
    end,npc)
    menu:addOption(settings(player).confirmPlans and getText("ContextMenu_PersonalKnox_ConfirmationOff") or getText("ContextMenu_PersonalKnox_ConfirmationOn"),player,function(p)
        settings(p).confirmPlans=not settings(p).confirmPlans
        p:Say(settings(p).confirmPlans and getText("ContextMenu_PersonalKnox_ConfirmationEnabled") or getText("ContextMenu_PersonalKnox_ConfirmationDisabled"))
    end)
    if Supply.pending[npc] then
        menu:addOption(getText("ContextMenu_PersonalKnox_ApprovePlan"),player,execute,npc)
        menu:addOption(getText("ContextMenu_PersonalKnox_DeclinePlan"),player,function(p,n)
            Supply.pending[n]=nil
            local square=p:getCurrentSquare()
            Supply.visited[n]=square and square:getBuilding()
            deps.journal(p,n,"supply_declined","")
        end,npc)
    end
end

function Supply.install(adapters)
    deps=adapters
    Events.EveryTenMinutes.Add(function()
        local player=getSpecificPlayer(0)
        if not player or isClient() or not PersonalKnoxAPI then return end
        local square=player:getCurrentSquare()
        local building=square and square:getBuilding()
        if not building then return end
        for npc,enabled in pairs(Supply.enabled) do
            if enabled and not Supply.pending[npc] and Supply.visited[npc]~=building and valid(player,npc,building) then
                local ok,err=pcall(Supply.propose,player,npc)
                if not ok then print("[PersonalKnox] Supply planner error: "..tostring(err)) end
            end
        end
    end)
end
return Supply
