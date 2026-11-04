require "KnoxEventLoader"
local Core = require "PersonalKnox/Core"
local Dialogues = nil -- Phrases are loaded by Java from the SQLite export.
PersonalKnox = PersonalKnox or {}
local Supply = require "PersonalKnox/Supplies"

local function attempt(fn, fallback)
    local ok, result = pcall(fn)
    if ok and result ~= nil then return result end
    return fallback
end

local function api()
    return attempt(function() return KnoxEventLoader.getAPI("npc") end, nil)
end

local function message(player, text)
    if player then player:Say(text) end
end

local function isMember(player, npc)
    return attempt(function()
        local service = api()
        local group = service and service:getGroup(player)
        return group ~= nil and group:inGroup(npc)
    end, false)
end

local function stateFor(player, npc)
    local service = api()
    local metaPlayer = service and attempt(function() return service:getMetaCharacter(player) end, nil)
    return {
        morale = attempt(function() return npc:getMorale() end, nil),
        relationship = metaPlayer and attempt(function() return npc:getRelationship(metaPlayer) end, nil),
        bravery = attempt(function() return npc:getPersonality():getBravery() end, nil),
    }
end

local function journal(player, npc, kind, detail)
    local data = player:getModData()
    data.PersonalKnox = data.PersonalKnox or { journal = {} }
    data.PersonalKnox.journal = data.PersonalKnox.journal or {}
    Core.appendJournal(data.PersonalKnox.journal, {
        hour = getGameTime():getWorldAgeHours(),
        npc = npc:getFullName(), kind = kind, detail = detail,
    }, 100)
end

local function say(player, npc, topic)
    if not isMember(player, npc) then return end
    local body = npc:getWorldObject()
    if not body or body:isDead() then
        message(player, "Нужно подойти к этому выжившему, чтобы поговорить.")
        return
    end
    if math.abs(body:getZ() - player:getZ()) > 0.5 or
        Core.distanceSquared({x=body:getX(), y=body:getY()}, {x=player:getX(), y=player:getY()}) > 100 then
        message(player, "Слишком далеко для разговора.")
        return
    end
    local state = stateFor(player, npc)
    local row = Core.pickDialogue(Dialogues, topic, state, ZombRand(10000))
    if row then
        body:Say(row.text)
        journal(player, npc, "dialogue", tostring(row.id))
    end
end

local function setJob(player, npc, job)
    if not isMember(player, npc) then return end
    local service = api()
    if not service or not service:getPlayerSafehouse() then
        message(player, "Сначала назначь убежище и рабочие зоны в меню Knox.")
        return
    end
    local ok, errorText = pcall(function()
        local priorities = npc:getJobPrioritizer()
        priorities:setPrio(job, NpcJobPriority.CRITICAL)
    end)
    if not ok then
        print("[PersonalKnox] Job unavailable: " .. tostring(errorText))
        message(player, "Приоритет не изменён: подробности в console.txt.")
        return
    end
    journal(player, npc, "job_priority", job)
    message(player, "Приоритет работы изменён. Для выполнения нужны зона, инструменты и смена работы Knox.")
end

local function report(player, npc)
    if not isMember(player, npc) then return end
    local body = npc:getWorldObject()
    if not body then message(player, "Этот выживший сейчас вне загруженной области."); return end
    local weapon = body:getPrimaryHandItem()
    local origin = {x=body:getX(), y=body:getY(), z=body:getZ()}
    local ranged = weapon and attempt(function() return weapon:isRanged() end, false)
    local range = ranged and attempt(function() return weapon:getMaxRange() end, 20) or 20
    local candidates = {below=0, above=0, same_floor=0, visibleBelow=0, unknownVisibility=0}
    local list = getCell():getZombieList()
    for i=0,list:size()-1 do
        local target = list:get(i)
        if not target:isDead() then
            local kind = Core.classifyTarget(origin, {x=target:getX(), y=target:getY(), z=target:getZ()}, range)
            if candidates[kind] ~= nil then
                candidates[kind] = candidates[kind] + 1
                if kind == "below" then
                    local visible = attempt(function() return body:CanSee(target) end, nil)
                    if visible == nil then candidates.unknownVisibility = candidates.unknownVisibility + 1
                    elseif visible then candidates.visibleBelow = candidates.visibleBelow + 1 end
                end
            end
        end
    end
    local ammo = ranged and attempt(function() return weapon:getCurrentAmmoCount() end, -1) or -1
    local result = string.format("npc=%s z=%.1f ranged=%s ammo=%s range=%s below=%d visibleBelow=%d unknownVisibility=%d sameFloor=%d above=%d",
        npc:getFullName(), origin.z, tostring(ranged), tostring(ammo), tostring(range), candidates.below,
        candidates.visibleBelow, candidates.unknownVisibility, candidates.same_floor, candidates.above)
    print("[PersonalKnox][elevation] " .. result)
    journal(player, npc, "elevation_diagnostic", result)
    message(player, string.format("Этаж %.0f: зомби снизу — %d, видимых — %d. Отчёт в console.txt.", origin.z + 1, candidates.below, candidates.visibleBelow))
end

local function roster(player)
    local result, seen = {}, {}
    local function add(npc)
        if npc and not seen[npc] and isMember(player, npc) then
            seen[npc] = true
            result[#result+1] = npc
        end
    end
    for _, npc in ipairs(selectedGroupNpcs or {}) do add(npc) end
    local service = api()
    local group = service and service:getGroup(player)
    if group then
        local members = group:getSafehouseNpcMembers()
        if members then for i=0,members:size()-1 do add(members:get(i)) end end
    end
    -- Nearby followers are not always listed among safehouse workers.
    local cell = getCell()
    for x=math.floor(player:getX())-8,math.floor(player:getX())+8 do
        for y=math.floor(player:getY())-8,math.floor(player:getY())+8 do
            local square = cell:getGridSquare(x, y, math.floor(player:getZ()))
            if square then
                local moving = square:getMovingObjects()
                for i=0,moving:size()-1 do
                    local body = moving:get(i)
                    if instanceof(body, "IsoNpcPlayer") then add(body:getMetaCharacter()) end
                end
            end
        end
    end
    return result
end

local function onContext(playerIndex, context, worldObjects, test)
    if test or isClient() or not classFilesInstalled or not PersonalKnoxAPI then return end
    local player = getSpecificPlayer(playerIndex)
    if not player or not api() then return end
    local members = roster(player)
    if #members == 0 then return end
    local root = context:addOption("Наш отряд", nil, nil)
    local menu = context:getNew(context)
    context:addSubMenu(root, menu)
    for _, npc in ipairs(members) do
        local entry = menu:addOption(npc:getFullName(), nil, nil)
        local sub = context:getNew(context)
        menu:addSubMenu(entry, sub)
        sub:addOption("Как ты держишься?", player, say, npc, "status")
        sub:addOption("Обсудить огород", player, say, npc, "farming")
        sub:addOption("Обсудить запасы", player, say, npc, "supplies")
        sub:addOption("Обсудить оборону с высоты", player, say, npc, "watch")
        sub:addOption("Обсудить отступление отряда", player, say, npc, "squad")
        Supply.addOptions(sub,player,npc)
        sub:addOption("Работа: земледелие — высший приоритет", player, setJob, npc, "Farming")
        sub:addOption("Работа: рубка дерева — высший приоритет", player, setJob, npc, "Chopping")
        sub:addOption("Работа: сортировка склада — высший приоритет", player, setJob, npc, "Organizing")
        sub:addOption("Диагностика: стрельба с высоты", player, report, npc)
    end
end

Supply.install({isMember=isMember,say=say,journal=journal})
Events.OnFillWorldObjectContextMenu.Add(function(...)
    local ok, errorText = pcall(onContext, ...)
    if not ok then print("[PersonalKnox] Context menu error: " .. tostring(errorText)) end
end)
print("[PersonalKnox] Lua addon loaded; Russian dialogues and elevation diagnostics ready.")
