-- Detect a squad member that follows but does not move, then recall it.
local Stuck = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local Util = require "PersonalKnox/Util"
local Call = require "PersonalKnox/Call"
local INTERVAL_MS, STUCK_MS = 5000, 20000
local lastCheck = 0
local seen = {}

local function follows(npc)
    local goal = tostring(npc:getGoal() or "")
    local sub = tostring(npc:getSubGoal() or "")
    return goal:find("Follow", 1, true) or goal:find("Roam", 1, true)
        or sub:find("Follow", 1, true) or sub:find("Roam", 1, true)
end

local function busy(body)
    local ok, value = pcall(function() return body:hasTimedActions() or body:isAttacking() end)
    if ok and value then return true end
    local combat = pcall(function() return body:isInActiveCombat() end)
    return combat
end

local function poll(player, roster, isMember)
    if isClient() or not PersonalKnoxAPI or not classFilesInstalled then return end
    local now = getTimestampMs()
    if now - lastCheck < INTERVAL_MS then return end
    lastCheck = now
    if Perf.essentialSkip() then return end
    for _, npc in ipairs(roster(player)) do
        local body = npc:getWorldObject()
        if body and not body:isDead() and isMember(player, npc) and follows(npc) and not busy(body) then
            local dx, dy = body:getX() - player:getX(), body:getY() - player:getY()
            if dx * dx + dy * dy > 16 then
                local state = seen[npc]
                if not state then
                    seen[npc] = {x = body:getX(), y = body:getY(), since = now}
                else
                    local moved = (body:getX() - state.x) ^ 2 + (body:getY() - state.y) ^ 2
                    if now - state.since >= STUCK_MS and moved < 0.25 then
                        if Call.one(player, npc, true) then
                            if Settings.diagnosticsEnabled() then
                                print("[PersonalKnox][stuck] npc=" .. tostring(Util.id(npc))
                                    .. " goal=" .. tostring(npc:getGoal()) .. " recall")
                            end
                        end
                        seen[npc] = nil
                    elseif moved >= 0.25 then
                        seen[npc] = {x = body:getX(), y = body:getY(), since = now}
                    end
                end
            else
                seen[npc] = nil
            end
        end
    end
end

function Stuck.install(deps)
    Events.OnGameStart.Add(function() lastCheck = 0; seen = {} end)
    Events.OnPlayerUpdate.Add(function()
        if not Settings.autoUnstick() then return end
        local player = getSpecificPlayer(0)
        if not player or player:isDead() then return end
        local ok, err = pcall(poll, player, deps.roster, deps.isMember)
        if not ok then print("[PersonalKnox] Stuck poll error: " .. tostring(err)) end
    end)
end

return Stuck
