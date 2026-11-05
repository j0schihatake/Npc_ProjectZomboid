-- Explicit recall, using Knox's public Lua command and group transfer API.
local Call = {}
local deps
local function canCall(player, npc)
    if isClient() or not classFilesInstalled or not player or player:isDead() or not npc or
        not deps.isMember(player, npc) then return false end
    local body = npc:getWorldObject()
    if not body or body:isDead() then return false end
    local dx, dy = body:getX() - player:getX(), body:getY() - player:getY()
    return dx * dx + dy * dy <= 900
end

function Call.one(player, npc, quiet)
    if not canCall(player, npc) or not tellNpcFollow then return false end
    local ok, err = pcall(function()
        local group = KnoxEventLoader.getAPI("npc"):getGroup(player)
        if not group then error("No player group") end
        -- Cancel our pending/autonomous work before it can issue another command.
        deps.cancelTasks(npc)
        group:sendToGroup(npc)
        npc:getWorldObject():StopAllActionQueue()
        tellNpcFollow(nil, npc)
        deps.journal(player, npc, "recall", "follow")
    end)
    if not ok then
        print("[PersonalKnox] Recall error: " .. tostring(err))
        return false
    end
    if not quiet then player:Say(getText("ContextMenu_PersonalKnox_CallName", npc:getFullName())) end
    return true
end

function Call.all(player)
    if isClient() or not classFilesInstalled or not player or player:isDead() then return end
    local count, seen = 0, {}
    for _, npc in ipairs(deps.roster(player)) do
        if not seen[npc] and count < 16 then
            seen[npc] = true
            if Call.one(player, npc, true) then count = count + 1 end
        end
    end
    if count > 0 then player:Say(getText("ContextMenu_PersonalKnox_CallAllVoice")) end
end

function Call.addOption(menu, player, npc)
    menu:addOption(getText("ContextMenu_PersonalKnox_CallOne"), player, Call.one, npc)
end

function Call.install(adapters)
    deps = adapters
    Events.OnKeyPressed.Add(function(key)
        local core = getCore()
        if isClient() or not classFilesInstalled or core:isDoingTextEntry() or
            not core:isKey("PersonalKnoxRecall", key) then return end
        local ok, err = pcall(Call.all, getSpecificPlayer(0))
        if not ok then print("[PersonalKnox] Recall key error: " .. tostring(err)) end
    end)
end
return Call
