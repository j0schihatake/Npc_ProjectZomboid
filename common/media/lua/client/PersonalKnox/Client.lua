pcall(require, "KnoxEventLoader")
local Core = require "PersonalKnox/Core"
local Version = require "PersonalKnox/Version"
local Probe = require "PersonalKnox/Probe"
local Dialogues = nil -- Phrases are loaded by Java from the SQLite export.
PersonalKnox = PersonalKnox or {}
classFilesInstalled = true
local Supply = require "PersonalKnox/Supplies"
local Reactions = require "PersonalKnox/Reactions"
local Routine = require "PersonalKnox/BaseRoutine"
local Interaction = require "PersonalKnox/Interaction"
local Call = require "PersonalKnox/Call"
local Weapons = require "PersonalKnox/Weapons"
local Barricade = require "PersonalKnox/Barricade"
local Settings = require "PersonalKnox/Settings"
local Util = require "PersonalKnox/Util"
local Care = require "PersonalKnox/Care"
local Upkeep = require "PersonalKnox/Upkeep"
local Stuck = require "PersonalKnox/Stuck"
local Crowd = require "PersonalKnox/Crowd"
local Help = require "PersonalKnox/Help"
local HelpCall = require "PersonalKnox/HelpCall"
local Negotiation = require "PersonalKnox/Negotiation"
local Chatter = require "PersonalKnox/Chatter"
local Perf = require "PersonalKnox/Perf"
local AutoAsk = require "PersonalKnox/AutoAsk"
local NpcCare = require "PersonalKnox/NpcCare"
local NpcEat = require "PersonalKnox/NpcEat"
local AutoReload = require "PersonalKnox/AutoReload"
local NpcReload = require "PersonalKnox/NpcReload"
local NpcMagazine = require "PersonalKnox/NpcMagazine"
local NpcUpkeep = require "PersonalKnox/NpcUpkeep"
local NpcMedic = require "PersonalKnox/NpcMedic"
local OwnNpcMedic = require "PersonalKnox/OwnNpcMedic"
local NpcStock = require "PersonalKnox/NpcStock"
local NpcFinds = require "PersonalKnox/NpcFinds"
local Ask = require "PersonalKnox/Ask"
local Alife = require "PersonalKnox/Alife"
local AmmoWeight = require "PersonalKnox/AmmoWeight"
local OwnNpc = require "PersonalKnox/OwnNpc"
local OwnNpcBodies = require "PersonalKnox/OwnNpcBodies"
local OwnNpcDebug = require "PersonalKnox/OwnNpcDebug"
local TextFix = require "PersonalKnox/TextFix"

local function attempt(fn, fallback)
    local ok, result = pcall(fn)
    if ok and result ~= nil then return result end
    return fallback
end

local function api()
    if not KnoxEventLoader then return nil end
    return attempt(function() return KnoxEventLoader.getAPI("npc") end, nil)
end

local function message(player, text)
    if not player then return end
    if PersonalKnoxAPI then
        local ok = pcall(function() PersonalKnoxAPI.getInstance():ownSpeak(player, text) end)
        if ok then return end
    end
    player:Say(text)
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
        npc = Util.id(npc), kind = kind, detail = detail,
    }, 100)
end

local function speak(body, text)
    if not PersonalKnoxAPI then return end
    pcall(function() PersonalKnoxAPI.getInstance():ownSpeak(body, text) end)
end

local function voice(body, topic)
    if not Settings.npcVoices() then return end
    local prefix = "VoiceMale"
    local ok, female = pcall(function() return body:isFemale() end)
    if ok and female then prefix = "VoiceFemale" end
    local sound = prefix
    if topic == "react_injury" or topic == "react_player_injury" then
        sound = prefix .. "PainFromHitBlunt"
    elseif topic:find("zombies", 1, true) or topic == "react_horde" or topic == "help_call" then
        sound = prefix .. "ShoutHey"
    end
    local played = pcall(function() body:playSound(sound) end)
    if Settings.diagnosticsEnabled() then
        print("[PersonalKnox][voice] sound=" .. sound .. " ok=" .. tostring(played))
    end
end

local function say(player, npc, topic)
    if not Settings.speechEnabled() then return end
    if not isMember(player, npc) then return end
    local body = npc:getWorldObject()
    if not body or body:isDead() then
        message(player, getText("ContextMenu_PersonalKnox_ApproachNpc"))
        return
    end
    if math.abs(body:getZ() - player:getZ()) > 0.5 or
        Core.distanceSquared({x=body:getX(), y=body:getY()}, {x=player:getX(), y=player:getY()}) > 100 then
        message(player, getText("ContextMenu_PersonalKnox_TooFar"))
        return
    end
    local state = stateFor(player, npc)
    local row = Core.pickDialogue(Dialogues, topic, state, ZombRand(10000))
    if row then
        speak(body, row.text)
        voice(body, topic)
        journal(player, npc, "dialogue", tostring(row.id))
    end
end

local function setJob(player, npc, job)
    if not isMember(player, npc) then return end
    local service = api()
    if not service or not service:getPlayerSafehouse() then
        message(player, getText("ContextMenu_PersonalKnox_NeedSafehouse"))
        return
    end
    local ok, errorText = pcall(function()
        local priorities = npc:getJobPrioritizer()
        priorities:setPrio(job, NpcJobPriority.CRITICAL)
    end)
    if not ok then
        print("[PersonalKnox] Job unavailable: " .. tostring(errorText))
        message(player, getText("ContextMenu_PersonalKnox_JobFailed"))
        return
    end
    journal(player, npc, "job_priority", job)
    message(player, getText("ContextMenu_PersonalKnox_JobChanged"))
end

local function report(player, npc)
    if not Settings.diagnosticsEnabled() then return end
    if not isMember(player, npc) then return end
    local body = npc:getWorldObject()
    if not body then message(player, getText("ContextMenu_PersonalKnox_NpcUnloaded")); return end
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
    message(player, getText("ContextMenu_PersonalKnox_ElevationSummary", tostring(math.floor(origin.z + 1)), tostring(candidates.below), tostring(candidates.visibleBelow)))
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
    local px,py,pz=math.floor(player:getX()),math.floor(player:getY()),math.floor(player:getZ())
    local processed=0
    for x=px-8,px+8 do
        for y=py-8,py+8 do
            local square = cell:getGridSquare(x, y, pz)
            if square then
                local moving = square:getMovingObjects()
                for i=0,moving:size()-1 do
                    processed=processed+1
                    local body = moving:get(i)
                    if instanceof(body, "IsoNpcPlayer") then add(body:getMetaCharacter()) end
                    -- Known group members above are retained even in a saturated scene.
                    if processed>=512 then return result end
                end
            end
        end
    end
    return result
end

local function fillNpcMenu(sub, player, npc)
    Call.addOption(sub, player, npc)
    Barricade.addOption(sub, player, npc)
    Weapons.addOptions(sub, player, npc)
    if Settings.speechEnabled() then
        sub:addOption(getText("ContextMenu_PersonalKnox_TalkStatus"), player, say, npc, "status")
        sub:addOption(getText("ContextMenu_PersonalKnox_TalkFarming"), player, say, npc, "farming")
        sub:addOption(getText("ContextMenu_PersonalKnox_TalkSupplies"), player, say, npc, "supplies")
        sub:addOption(getText("ContextMenu_PersonalKnox_TalkWatch"), player, say, npc, "watch")
        sub:addOption(getText("ContextMenu_PersonalKnox_TalkRetreat"), player, say, npc, "squad")
    end
    Supply.addOptions(sub,player,npc)
    Routine.addOptions(sub,player,npc)
    sub:addOption(getText("ContextMenu_PersonalKnox_JobFarming"), player, setJob, npc, "Farming")
    sub:addOption(getText("ContextMenu_PersonalKnox_JobChopping"), player, setJob, npc, "Chopping")
    sub:addOption(getText("ContextMenu_PersonalKnox_JobOrganizing"), player, setJob, npc, "Organizing")
    if Settings.diagnosticsEnabled() then
        sub:addOption(getText("ContextMenu_PersonalKnox_ElevationReport"), player, report, npc)
    end
end

local function onContext(playerIndex, context, worldObjects, test)
    if test or isClient() or not classFilesInstalled or not PersonalKnoxAPI then return end
    local player = getSpecificPlayer(playerIndex)
    if not player or not api() then return end
    local members = roster(player)
    local apiRef = PersonalKnoxAPI.getInstance()
    local ownTotal = apiRef:ownCount()
    local lookedOwn = false
    for _, obj in ipairs(worldObjects or {}) do
        if instanceof(obj, "IsoPlayer") then
            local okName, name = pcall(function() return obj:getUsername() end)
            if okName and type(name) == "string" and name:sub(1, 3) == "PK-" then
                lookedOwn = true
                break
            end
        end
    end
    if ownTotal > 0 and not lookedOwn then
        local root = context:addOption("Свой отряд", nil, nil)
        local menu = context:getNew(context)
        context:addSubMenu(root, menu)
        local function all(mode)
            apiRef:ownCommandAll(mode, player:getX(), player:getY(), player:getZ())
        end
        menu:addOption("Собирать всем", player, function() all("LOOT") end)
        menu:addOption("Отдыхать всем", player, function() all("REST") end)
        menu:addOption("Следовать всем", player, function() all("FOLLOW") end)
        menu:addOption("Тревога базы", player, function()
            message(player, apiRef:ownBaseAlarm(3))
        end)
        menu:addOption("Отбой", player, function()
            message(player, apiRef:ownBaseAlarm(0))
        end)
        menu:addOption("Статус базы", player, function()
            message(player, apiRef:ownBaseStatus())
        end)
        menu:addOption("Медик", player, function()
            message(player, apiRef:ownAssignMedic(true))
        end)
        menu:addOption("Раненые", player, function()
            message(player, apiRef:ownRotateWounded())
        end)
        menu:addOption("Построить ферму", player, function()
            message(player, apiRef:ownBuildFacility("FARM"))
        end)
        menu:addOption("Построить воду", player, function()
            message(player, apiRef:ownBuildFacility("WATER"))
        end)
        menu:addOption("Построить кухню", player, function()
            message(player, apiRef:ownBuildFacility("KITCHEN"))
        end)
        menu:addOption("Построить лазарет", player, function()
            message(player, apiRef:ownBuildFacility("INFIRMARY"))
        end)
        menu:addOption("Производство", player, function()
            message(player, apiRef:ownFacilityTick())
        end)
        for _, spec in ipairs({{"GENERATOR","Генератор"},{"WATCHTOWER","Вышка"},{"WORKSHOP","Мастерская"},{"LAUNDRY","Прачечная"},{"ARMORY","Арсенал"},{"GARAGE","Гараж"},{"RADIO","Радио"},{"TRAINING","Тренировка"},{"DORM","Спальня"},{"STORAGE","Склад"}}) do
            local kind, label = spec[1], spec[2]
            menu:addOption("Построить: " .. label, player, function()
                message(player, apiRef:ownBuildFacility(kind))
            end)
        end
        local buildEntry = menu:addOption("Все объекты базы", nil, nil)
        local buildMenu = context:getNew(context)
        menu:addSubMenu(buildEntry, buildMenu)
        for _, kind in ipairs({"FARM","WATER","KITCHEN","WORKSHOP","INFIRMARY","LAUNDRY","GENERATOR","WATCHTOWER","WALL","GATE","STORAGE","GARAGE","RADIO","TRAINING","ARMORY","DORM","LATRINE","SHOWER","FISH_DOCK","HUNT_BLIND","MORGUE","KILN","WELL","SMOKEHOUSE","BUTCHER","BAKERY","BREWERY","TAILOR_SHOP","WORKBENCH","WATER_TOWER"}) do
            buildMenu:addOption(kind, player, function()
                message(player, apiRef:ownBuildFacility(kind))
            end)
        end
        menu:addOption("Оружие всем", player, function()
            message(player, "Достали оружие: " .. tostring(apiRef:ownDrawAll()))
        end)
        menu:addOption("Убрать оружие всем", player, function()
            message(player, "Убрали оружие: " .. tostring(apiRef:ownHolsterAll()))
        end)
        menu:addOption("База здесь", player, function()
            apiRef:ownBaseSet(player:getX(), player:getY(), player:getZ())
            local count = apiRef:ownBaseGuardAll()
            message(player, "База: охраняют " .. tostring(count))
        end)
        menu:addOption("Открыть окно отряда", player, function()
            local ok, panel = pcall(require, "PersonalKnox/OwnNpcPanel")
            if ok and panel then panel.toggle(player) end
        end)
        menu:addOption("Все за мной", player, function() all("FOLLOW") end)
        menu:addOption("Все стоять", player, function() all("HOLD") end)
        menu:addOption("Все охранять", player, function() all("GUARD") end)
    end
    local seenOwn = {}
    for _, obj in ipairs(worldObjects or {}) do
        if instanceof(obj, "IsoPlayer") then
            local okName, name = pcall(function() return obj:getUsername() end)
            if okName and type(name) == "string" and name:sub(1, 3) == "PK-" then
                local npcId = name:sub(4)
                if npcId ~= "" and not seenOwn[npcId] then
                    seenOwn[npcId] = true
                    local entry = context:addOption("Свой NPC: " .. npcId, nil, nil)
                    local sub = context:getNew(context)
                    context:addSubMenu(entry, sub)
                    sub:addOption("Следовать за мной", player, function()
                        apiRef:ownCommand(npcId, "FOLLOW", player:getX(), player:getY(), player:getZ())
                    end)
                    sub:addOption("Стоять", player, function()
                        apiRef:ownCommand(npcId, "HOLD", player:getX(), player:getY(), player:getZ())
                    end)
                    sub:addOption("Охранять", player, function()
                        apiRef:ownCommand(npcId, "GUARD", player:getX(), player:getY(), player:getZ())
                    end)
                    sub:addOption("Собирать", player, function()
                        apiRef:ownCommand(npcId, "LOOT", player:getX(), player:getY(), player:getZ())
                    end)
                    sub:addOption("Отдыхать", player, function()
                        apiRef:ownCommand(npcId, "REST", player:getX(), player:getY(), player:getZ())
                    end)
                    sub:addOption("Состояние", player, function()
                        message(player, apiRef:ownSummary(npcId))
                    end)
                    sub:addOption("Поговорить", player, function()
                        if apiRef:ownSpeakById(npcId, "На связи.") then
                            message(player, "NPC ответил.")
                        end
                    end)
                    sub:addOption("Взять лучшее оружие", player, function()
                        message(player, "Оружие: " .. tostring(apiRef:ownAutoEquip(npcId)))
                    end)
                    sub:addOption("Достать оружие", player, function()
                        message(player, "Достал: " .. tostring(apiRef:ownDraw(npcId)))
                    end)
                    sub:addOption("Убрать оружие", player, function()
                        message(player, "Убрал: " .. tostring(apiRef:ownHolster(npcId)))
                    end)
                    sub:addOption("Инвентарь", player, function()
                        message(player, "Инвентарь: " .. tostring(apiRef:ownInventory(npcId)))
                    end)
                    sub:addOption("Отдать одежду", player, function()
                        message(player, "Надето: " .. tostring(apiRef:ownGiveClothing(npcId, player)))
                    end)
                    sub:addOption("Отдать оружие", player, function()
                        message(player, "Передано: " .. tostring(apiRef:ownGiveWeapon(npcId, player)))
                    end)
                    sub:addOption("Забрать предметы", player, function()
                        local moved = apiRef:ownGiveItems(npcId, player)
                        message(player, "Получено предметов: " .. tostring(moved))
                    end)
                    sub:addOption("Разобрать рядом", player, function()
                        print("[PersonalKnox][own-menu] dismantle=" .. tostring(apiRef:ownDismantleStart(npcId)))
                    end)
                    local profEntry = sub:addOption("Профессия", nil, nil)
                    local profMenu = context:getNew(context)
                    sub:addSubMenu(profEntry, profMenu)
                    for _, profession in ipairs({"FARMER","WATER","FIRE","CORPSE","CLEAN","COOK","LAUNDRY","FISH","HUNT","SCAVENGE","CRAFT","BUILD","GUARD","PATROL","ENFORCER","DRIVER","MEDIC","MECHANIC","ELECTRICIAN","METALWORKER","TAILOR","BREWER","HERBALIST","TRAPPER","LOOKOUT","QUARTERMASTER","DOCTOR","NURSE","ENGINEER","ARCHITECT","FARMHAND","RANCHER","BUTCHER","BAKER","SMITH","POTTER","SCRIBE","TEACHER","PRIEST","TRADER","COURIER","WATCHMAN","SAPPER"}) do
                        profMenu:addOption(profession, player, function()
                            message(player, "Профессия: " .. tostring(apiRef:ownSetProfession(npcId, profession)))
                        end)
                    end
                    sub:addOption("Осмотреть", player, function()
                        local summary = tostring(apiRef:ownSummary(npcId))
                        print("[PersonalKnox][own-menu] " .. summary)
                        if player and player.Say then player:Say(summary) end
                    end)
                end
            end
        end
    end
    if #members == 0 then return end
    local root = context:addOption(getText("ContextMenu_PersonalKnox_Squad"), nil, nil)
    local menu = context:getNew(context)
    context:addSubMenu(root, menu)
    Reactions.addOption(menu, player)
    Negotiation.addOptions(menu, player)
    menu:addOption(getText("ContextMenu_PersonalKnox_CallAll"), player, Call.all)
    for _, npc in ipairs(members) do
        local entry = menu:addOption(npc:getFullName(), nil, nil)
        local sub = context:getNew(context)
        menu:addSubMenu(entry, sub)
        fillNpcMenu(sub, player, npc)
    end
end

Supply.install({isMember=isMember,say=say,journal=journal})
Routine.install({isMember=isMember})
Interaction.install({isMember=isMember,fill=fillNpcMenu,reactions=Reactions.addOption})
Call.install({isMember=isMember,roster=roster,journal=journal,cancelTasks=function(npc)
    Supply.cancel(npc)
    Barricade.cancel(npc)
end})
Barricade.install({isMember=isMember,journal=journal,say=say,cancelSupply=Supply.cancel,stockSnapshot=Supply.stockSnapshot})
Weapons.install({isMember=isMember})
Reactions.install({isMember=isMember,say=say})
Events.OnFillWorldObjectContextMenu.Add(function(...)
    local ok, errorText = pcall(onContext, ...)
    if not ok then print("[PersonalKnox] Context menu error: " .. tostring(errorText)) end
end)
Care.install()
Upkeep.install()
Stuck.install({isMember=isMember,roster=roster})
Crowd.install({isMember=isMember})
Help.install()
HelpCall.install({roster=roster,say=say})
Negotiation.install({roster=roster,say=say})
Chatter.install({roster=roster,say=say})
Perf.install()
AutoAsk.install({roster=roster,say=say})
Alife.install({roster=roster,stockSnapshot=Supply.stockSnapshot})
AmmoWeight.install({roster=roster})
OwnNpc.install({roster=roster})
local OwnFactions = require "PersonalKnox/OwnFactions"
OwnFactions.install()
local function applyWeaponSetting()
    local ok, api = pcall(function() return PersonalKnoxAPI.getInstance() end)
    if ok and api then
        pcall(function() api:ownSetStartingWeapon(Settings.npcWeapon()) end)
        pcall(function() api:ownSetFactionChances(Settings.factionRanged(), Settings.factionMelee()) end)
    end
end
Events.OnGameStart.Add(applyWeaponSetting)

OwnNpcDebug.install()
TextFix.install()
Events.OnGameStart.Add(function()
    if not Settings.openSquad() then return end
    local player = getSpecificPlayer(0)
    if not player then return end
    local ok, panel = pcall(require, "PersonalKnox/OwnNpcPanel")
    if ok and panel then panel.toggle(player) end
end)
Events.OnGameStart.Add(function()
    local player = getSpecificPlayer(0)
    if player and HaloTextHelper then
        pcall(function()
            HaloTextHelper.addGoodText(player, getText("UI_PersonalKnox_StartHint"))
            print("[PersonalKnox][hint] shown=true")
        end)
    end
end)
Events.OnGameStart.Add(function()
    if not Settings.inGameTests() then return end
    local player = getSpecificPlayer(0)
    if not player then return end
    local due = getTimestampMs() + 2000
    Events.OnPlayerUpdate.Add(function()
        if getTimestampMs() < due or player:getModData().PersonalKnoxTestsStarted then return end
        player:getModData().PersonalKnoxTestsStarted = true
        local ok, tests = pcall(require, "PersonalKnox/OwnNpcInGameTests")
        if ok and tests then tests.run(player) end
    end)
end)
NpcCare.install({roster=roster,say=say})
NpcEat.install({roster=roster,say=say})
AutoReload.install()
NpcReload.install({roster=roster,say=say})
NpcMagazine.install({roster=roster,say=say})
NpcUpkeep.install({roster=roster,say=say})
NpcMedic.install({roster=roster,say=say})
OwnNpcMedic.install()
NpcStock.install({roster=roster,say=say})
NpcFinds.install({roster=roster,say=say})
Ask.install({roster=roster,say=say})
Probe.install({roster=roster,isMember=isMember})
print("[PersonalKnox] Lua addon " .. Version.loaded .. " loaded; Russian dialogues and elevation diagnostics ready.")
