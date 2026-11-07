-- F10: spawn one own NPC to prove the Java body adapter without a Knox squad.
local OwnNpcDebug = {}
local lastId = nil

local function spawn()
    if isClient() or not PersonalKnoxAPI then return end
    local player = getSpecificPlayer(0)
    if not player or player:isDead() then return end
    local api = PersonalKnoxAPI.getInstance()
    local id = api:ownCreateCompanion(player)
    lastId = id
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
        elseif key == Keyboard.KEY_F7 then
            local ok, panel = pcall(require, "PersonalKnox/OwnNpcPanel")
            if ok and panel then panel.toggle(getSpecificPlayer(0)) end
        elseif key == Keyboard.KEY_F8 then
            local api = PersonalKnoxAPI.getInstance()
            for i = 0, api:ownCount() - 1 do
                print("[PersonalKnox][own-roster] " .. tostring(api:ownSummary(api:ownId(i))))
            end
        elseif key == Keyboard.KEY_F9 then
            if not lastId then return end
            local api = PersonalKnoxAPI.getInstance()
            local obstacle = api:ownNearbyObstacle(lastId)
            local plan = obstacle ~= "" and api:ownDismantlePlan(lastId, obstacle) or ""
            local ok, result = pcall(function() return api:ownDismantleStart(lastId) end)
            print("[PersonalKnox][own-debug] obstacle=" .. tostring(obstacle)
                .. " plan=" .. tostring(plan) .. " dismantle=" .. tostring(ok and result or "error"))
        end
    end)
end

return OwnNpcDebug
