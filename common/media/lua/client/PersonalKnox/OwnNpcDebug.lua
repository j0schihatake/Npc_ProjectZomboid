-- F10: spawn one own NPC to prove the Java body adapter without a Knox squad.
local OwnNpcDebug = {}

local function spawn()
    if isClient() or not PersonalKnoxAPI then return end
    local player = getSpecificPlayer(0)
    if not player or player:isDead() then return end
    local api = PersonalKnoxAPI.getInstance()
    local id = api:ownDebugSpawn(player:getX() + 1, player:getY() + 1, player:getZ())
    local bodies = api:ownDebugBodySync()
    print("[PersonalKnox][own-debug] id=" .. tostring(id) .. " bodies=" .. tostring(bodies))
end

function OwnNpcDebug.install()
    Events.OnKeyPressed.Add(function(key)
        local core = getCore()
        if core:isDoingTextEntry() then return end
        if key == Keyboard.KEY_F10 then
            local ok, err = pcall(spawn)
            if not ok then print("[PersonalKnox] Own debug error: " .. tostring(err)) end
        end
    end)
end

return OwnNpcDebug
