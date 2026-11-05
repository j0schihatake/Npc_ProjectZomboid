local Util = require "PersonalKnox/Util"
-- Explicit commands only. Knox owns movement, furniture and rest behaviour.
local Routine = {}
local adapters
local names = {"ANY", "WORK", "RECREATION", "REST", "GUARD"}

local function state(player)
    local data = player:getModData()
    data.PersonalKnox = data.PersonalKnox or {}
    data.PersonalKnox.baseSchedules = data.PersonalKnox.baseSchedules or {}
    return data.PersonalKnox.baseSchedules
end

local function encode(value)
    for _, name in ipairs(names) do
        if NpcSchedule[name] == value then return name end
    end
    error("Unknown Knox schedule; nothing changed")
end

local function snapshot(priorities)
    local result = {}
    for hour = 0, 23 do result[hour + 1] = encode(priorities:getScheduleForHour(hour)) end
    return result
end

local function validate(plan)
    assert(type(plan) == "table" and #plan == 24, "Invalid saved schedule")
    for hour = 1, 24 do
        local valid = false
        for _, name in ipairs(names) do if plan[hour] == name then valid = true end end
        assert(valid and NpcSchedule[plan[hour]] ~= nil, "Unknown saved schedule")
    end
end

local function write(priorities, plan)
    for hour = 0, 23 do priorities:setScheduleForHour(hour, NpcSchedule[plan[hour + 1]]) end
end

local function replace(priorities, plan, original)
    validate(plan)
    local ok, err = pcall(write, priorities, plan)
    if ok then return end
    local restored, rollbackError = pcall(write, priorities, original)
    if not restored then
        print("[PersonalKnox] Schedule rollback failed: " .. tostring(rollbackError))
    end
    error(tostring(err))
end

local function service()
    return KnoxEventLoader.getAPI("npc")
end

local function execute(player, npc, action)
    if isClient() or not adapters.isMember(player, npc) then return end
    local ok, err = pcall(action)
    if not ok then
        print("[PersonalKnox] Base routine error: " .. tostring(err))
        player:Say(getText("ContextMenu_PersonalKnox_RoutineFailed"))
    end
end

function Routine.apply(player, npc)
    execute(player, npc, function()
        if not service():getPlayerSafehouse() then
            player:Say(getText("ContextMenu_PersonalKnox_NeedSafehouse")); return
        end
        local priorities = npc:getJobPrioritizer()
        local original, plan = snapshot(priorities), {}
        for hour = 0, 23 do
            plan[hour + 1] = PersonalKnoxAPI.getInstance():baseSchedule(hour, original[hour + 1])
        end
        local backups, key = state(player), tostring(Util.id(npc))
        replace(priorities, plan, original)
        -- Reapplying must not overwrite the user's original schedule.
        if backups[key] == nil then backups[key] = original end
        player:Say(getText("ContextMenu_PersonalKnox_RoutineApplied"))
    end)
end

function Routine.restore(player, npc)
    execute(player, npc, function()
        local backups, key = state(player), tostring(Util.id(npc))
        local saved = backups[key]
        if saved == nil then return end
        local priorities = npc:getJobPrioritizer()
        replace(priorities, saved, snapshot(priorities))
        backups[key] = nil
        player:Say(getText("ContextMenu_PersonalKnox_RoutineRestored"))
    end)
end

function Routine.returnHome(player, npc)
    execute(player, npc, function()
        local api = service()
        if not api:getPlayerSafehouse() then
            player:Say(getText("ContextMenu_PersonalKnox_NeedSafehouse")); return
        end
        api:getGroup(player):sendToSafehouse(npc)
        player:Say(getText("ContextMenu_PersonalKnox_ReturningHome"))
    end)
end

function Routine.addOptions(menu, player, npc)
    menu:addOption(getText("ContextMenu_PersonalKnox_BaseRoutine"), player, Routine.apply, npc)
    if state(player)[tostring(Util.id(npc))] ~= nil then
        menu:addOption(getText("ContextMenu_PersonalKnox_RestoreRoutine"), player, Routine.restore, npc)
    end
    menu:addOption(getText("ContextMenu_PersonalKnox_ReturnHome"), player, Routine.returnHome, npc)
end

function Routine.install(deps) adapters = deps end
return Routine
