-- Native safehouse job adapter. No instant construction or independent pathfinding.
local Barricade = {active={}, cursor=0}
local Settings = require "PersonalKnox/Settings"
local Util = require "PersonalKnox/Util"
local deps
local MAX_TASKS, MAX_ROOMS, MAX_WINDOWS = 8, 64, 128

local function find(npc)
    for index, task in ipairs(Barricade.active) do
        if task.npc == npc then return task, index end
    end
end

function Barricade.cancel(npc)
    local _, index = find(npc)
    if index then table.remove(Barricade.active, index) end
end

local function api() return KnoxEventLoader.getAPI("npc") end
local function owns(task)
    return task.npc:getGoal() == "SafehouseSubTree" and task.npc:getSubGoal() == "BarricadeSubTree"
end

local function notice(player, key)
    if player and not player:isDead() then player:Say(getText("ContextMenu_PersonalKnox_Barricade" .. key)) end
end

local function windowsFor(building)
    local windows, seen = {}, {}
    local def = building:getDef()
    local rooms, levels = def:getRooms(), {}
    assert(rooms:size() <= MAX_ROOMS, "Building exceeds room budget")
    for i=0,rooms:size()-1 do
        local definition = rooms:get(i)
        local room = definition:getIsoRoom()
        assert(room, "Building has unloaded rooms")
        levels[definition:getZ()] = true
    end
    -- IsoRoom:getWindows() returned an empty cache in a live Viewpoint house.
    -- Inspect only this building's loaded bounds, including exterior wall squares.
    local x1,y1,x2,y2=def:getX()-1,def:getY()-1,def:getX2()+1,def:getY2()+1
    local floors=0
    for _ in pairs(levels) do floors=floors+1 end
    assert((x2-x1+1)*(y2-y1+1)*floors<=4096,"Building exceeds square budget")
    local cell, processed = getCell(), 0
    for z in pairs(levels) do for x=x1,x2 do for y=y1,y2 do
        local square=cell:getGridSquare(x,y,z)
        assert(square,"Building bounds not fully loaded")
        local objects=square:getObjects()
        for i=0,objects:size()-1 do
            processed=processed+1
            assert(processed<=32768,"Building exceeds object budget")
            local window=objects:get(i)
            if instanceof(window,"IsoWindow") and not seen[window] and window:isBarricadeAllowed() then
                local inside=window:getIndoorSquare()
                local owner=inside and inside:getBuilding()
                if owner and owner:getDef():equals(def) then
                    seen[window]=true
                    windows[#windows+1]=window
                    assert(#windows<=MAX_WINDOWS,"Building exceeds window budget")
                end
            end
        end
    end end end
    return windows
end

local function nativeNode(npc)
    local tree=npc:getBehaviorTree()
    local direct=tree:getChildWithName("BarricadeSubTree")
    if direct then return direct end
    local goal=tree:getGoalNode()
    if not goal then return nil end
    direct=goal:getChildWithNameWithDepth("BarricadeSubTree",1)
    if direct then return direct end
    local home=goal:getChildWithNameWithDepth("SafehouseSubTree",1)
    return home and home:getChildWithName("BarricadeSubTree") or nil
end

local function remaining(task)
    local total = 0
    local java = PersonalKnoxAPI.getInstance()
    for _, window in ipairs(task.windows) do
        -- A removed/unloaded window is not proof of a completed house.
        assert(window:getObjectIndex() ~= -1 and window:getSquare(), "Target window no longer loaded")
        local a, b = window:getBarricadeOnSameSquare(), window:getBarricadeOnOppositeSquare()
        local planks, metal = 0, false
        for _, side in pairs({a, b}) do
            planks = math.max(planks, side:getNumPlanks())
            metal = metal or side:isMetal() or side:isMetalBar()
        end
        total = total + java:barricadeMissing(planks, metal)
    end
    return total
end

local function topUp(task, missing)
    local inventory = task.npc:getWorldObject():getInventory()
    local java = PersonalKnoxAPI.getInstance()
    local function addTo(fullType, target)
        local count = inventory:getItemCount(fullType, true)
        for i=1,math.max(0,target-count) do
            assert(inventory:AddItem(fullType), "Cannot create " .. fullType)
        end
    end
    addTo("Base.Plank", java:barricadeStockTarget(missing, false))
    addTo("Base.Nails", java:barricadeStockTarget(missing, true))
    if missing > 0 and not inventory:getFirstTagEvalRecurse(ItemTag.HAMMER, function(item) return not item:isBroken() end) then
        assert(inventory:AddItem("Base.Hammer"), "Cannot create hammer")
    end
end

local function danger(body)
    local cell, processed = getCell(), 0
    local x, y, z = math.floor(body:getX()), math.floor(body:getY()), math.floor(body:getZ())
    for dx=-3,3 do for dy=-3,3 do
        local square = cell:getGridSquare(x+dx, y+dy, z)
        if square then
            local objects = square:getMovingObjects()
            for i=0,objects:size()-1 do
                processed = processed + 1
                local object = objects:get(i)
                if instanceof(object, "IsoZombie") and not object:isDead() and body:CanSee(object) then return true end
                if processed >= 128 then return true end
            end
        end
    end end
    return false
end

local function stop(task, reason, restoreStart)
    Barricade.cancel(task.npc)
    local body = task.npc:getWorldObject()
    if body and not body:isDead() and deps.isMember(task.player, task.npc) and (owns(task) or restoreStart) then
        body:StopAllActionQueue()
        if reason == "danger" or task.previousGoal == "RoamingSubTree" then
            api():getGroup(task.player):sendToGroup(task.npc)
            tellNpcFollow(nil, task.npc)
        else
            task.npc:setGoal(task.previousGoal)
            task.npc:setSubGoal(task.previousSubGoal)
        end
    end
    if Settings.diagnosticsEnabled() then
        print("[PersonalKnox][barricade] npc=" .. tostring(Util.id(task.npc)) .. " stop=" .. reason)
    end
    deps.journal(task.player, task.npc, "barricade_stop", reason)
    if reason == "done" then notice(task.player, "Done")
    elseif reason == "stalled" then notice(task.player, "Stalled")
    elseif reason ~= "interrupted" then notice(task.player, "Stopped") end
end

local function begin(player, npc)
    if isClient() or not classFilesInstalled or not PersonalKnoxAPI or not player or player:isDead() or
        not deps.isMember(player, npc) then return end
    local body = npc:getWorldObject()
    if not body or body:isDead() then return end
    local dx, dy = body:getX()-player:getX(), body:getY()-player:getY()
    if dx*dx+dy*dy > 100 then return end
    local square = player:getCurrentSquare()
    local building = square and square:getBuilding()
    local safehouse = api():getPlayerSafehouse()
    if not building or not safehouse or not safehouse:equals(building:getDef()) or
        not body:getCurrentSquare() or body:getCurrentSquare():getBuilding() ~= building then
        notice(player, "NeedHome"); return
    end
    if find(npc) then return end
    if #Barricade.active >= MAX_TASKS then notice(player, "Busy"); return end
    local windows = windowsFor(building)
    if #windows == 0 then notice(player, "NoWindows"); return end
    local task = {player=player,npc=npc,home=safehouse,building=building,windows=windows,
        previousGoal=npc:getGoal(),previousSubGoal=npc:getSubGoal(),lastProgress=getGameTime():getWorldAgeHours()}
    local missing = remaining(task)
    if missing == 0 then notice(player, "Done"); return end
    if danger(body) then notice(player, "Danger"); return end
    task.missing = missing
    -- Validate everything before taking over this NPC's task.
    deps.cancelSupply(npc)
    deps.say(player, npc, "plan_barricade")
    Barricade.active[#Barricade.active+1] = task
    api():getGroup(player):sendToSafehouse(npc)
    body:StopAllActionQueue()
    npc:setGoal("SafehouseSubTree")
    npc:setSubGoal("BarricadeSubTree")
    assert(nativeNode(npc),"Native barricading node unavailable after assignment")
    topUp(task, missing)
    deps.journal(player,npc,"barricade_start",tostring(#windows))
    if Settings.diagnosticsEnabled() then
        print("[PersonalKnox][barricade] npc=" .. tostring(Util.id(npc)) .. " windows=" .. #windows .. " missing=" .. missing)
    end
end

function Barricade.start(player, npc)
    local ok, err = pcall(begin, player, npc)
    if not ok then
        local task = find(npc)
        if task then pcall(stop, task, "error", true) end
        print("[PersonalKnox] Barricade start error: " .. tostring(err))
        notice(player, "Failed")
    end
end

local function update(task)
    local body = task.npc:getWorldObject()
    if not body or body:isDead() or task.player:isDead() or not deps.isMember(task.player,task.npc) then
        stop(task,"interrupted"); return
    end
    local home = api():getPlayerSafehouse()
    if not home or not home:equals(task.home) then stop(task,"interrupted"); return end
    -- A new native order wins. Never reissue our goal over it.
    if not owns(task) then stop(task,"interrupted"); return end
    local missing, now = remaining(task), getGameTime():getWorldAgeHours()
    if missing == 0 then
        task.windows = windowsFor(task.building)
        missing = remaining(task)
    end
    if missing < task.missing then task.lastProgress = now end
    task.missing = missing
    local state = PersonalKnoxAPI.getInstance():barricadeStatus(missing, true, danger(body), now-task.lastProgress)
    if state == "work" then topUp(task, missing)
    else stop(task, state) end
end

function Barricade.poll()
    if isClient() or not classFilesInstalled or not PersonalKnoxAPI or #Barricade.active == 0 then return end
    local batch, count = {}, #Barricade.active
    for offset=1,math.min(4,count) do batch[#batch+1]=Barricade.active[((Barricade.cursor+offset-1)%count)+1] end
    for _, task in ipairs(batch) do
        if find(task.npc) == task then
            local ok, err = pcall(update, task)
            if not ok then
                pcall(stop,task,"error")
                print("[PersonalKnox] Barricade update error: " .. tostring(err))
            end
        end
    end
    Barricade.cursor = (Barricade.cursor+4)%math.max(1,#Barricade.active)
end

function Barricade.inspect(player, npc)
    if not Settings.diagnosticsEnabled() then return end
    if isClient() or not player or not deps.isMember(player,npc) then return end
    local body = npc:getWorldObject()
    if not body then return end
    local fields = {}
    local function read(key, fn)
        local ok, value = pcall(fn)
        fields[#fields+1] = key .. "=" .. (ok and tostring(value) or "unavailable")
        if not ok then print("[PersonalKnox][probe] " .. key .. ": " .. tostring(value)) end
    end
    read("npc",function() return Util.id(npc) end)
    read("goal",function() return npc:getGoal() end)
    read("subGoal",function() return npc:getSubGoal() end)
    read("nativeBarricadeCurrent",function() return nativeNode(npc) ~= nil end)
    read("luaNpcTransferAction",function() return ISNpcInventoryTransferAction ~= nil end)
    read("luaBehaviorNode",function() return LuaBehaviorNode ~= nil end)
    read("luaSubTreeNode",function() return LuaSubTreeNode ~= nil end)
    read("noDangerousTargets",function() return body:hasNoDangerousTargets() end)
    read("enemyNpcTargets",function() return body:hasEnemyNpcTargets() end)
    read("activeCombat",function() return body:isInActiveCombat() end)
    read("preferredWeapon",function()
        local item=npc:getPreferredWeapon()
        return item and item:getFullType() or "none"
    end)
    read("health",function() return body:getBodyDamage():getHealth() end)
    read("hunger",function() return body:getStats():get(CharacterStat.HUNGER) end)
    read("thirst",function() return body:getStats():get(CharacterStat.THIRST) end)
    read("fatigue",function() return body:getStats():get(CharacterStat.FATIGUE) end)
    read("planks",function() return body:getInventory():getItemCount("Base.Plank",true) end)
    read("nails",function() return body:getInventory():getItemCount("Base.Nails",true) end)
    read("healthyHammer",function()
        return body:getInventory():getFirstTagEvalRecurse(ItemTag.HAMMER,function(item) return not item:isBroken() end) ~= nil
    end)
    read("safehouseMatches",function()
        local building = body:getCurrentSquare():getBuilding()
        local home = api():getPlayerSafehouse()
        return building ~= nil and home ~= nil and home:equals(building:getDef())
    end)
    read("windows",function() return #windowsFor(body:getCurrentSquare():getBuilding()) end)
    local report = table.concat(fields," ")
    print("[PersonalKnox][probe] " .. report)
    deps.journal(player,npc,"npc_probe",report)
    notice(player,"InspectLogged")
    return report
end

function Barricade.addOption(menu, player, npc)
    menu:addOption(getText("ContextMenu_PersonalKnox_BarricadeHome"),player,Barricade.start,npc)
    if Settings.diagnosticsEnabled() then
        menu:addOption(getText("ContextMenu_PersonalKnox_BarricadeInspect"),player,Barricade.inspect,npc)
    end
end
function Barricade.install(adapters)
    deps = adapters
    Events.OnGameStart.Add(function() Barricade.active={};Barricade.cursor=0 end)
    Events.EveryOneMinute.Add(Barricade.poll)
end
return Barricade
