-- Companions themselves ask/share food and water when someone is hungry or thirsty.
local AutoAsk = {}
local Settings = require "PersonalKnox/Settings"
local Perf = require "PersonalKnox/Perf"
local INTERVAL_MS, MAX_NPCS = 90000, 2
local lastCheck, deps = 0, nil

local function kindOf(item)
    if item:IsFood() then return "food" end
    local script = item:getScriptItem()
    if script and script:getThirstChange() < 0 then return "water" end
    return nil
end

local function findItem(body, kind)
    local items = body:getInventory():getItems()
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        if kindOf(item) == kind then return item end
    end
    return nil
end

local function share(player, donor, needy, item, kind)
    local container = item:getContainer()
    if not container or not ISInventoryTransferAction or not ISTimedActionQueue then return false end
    ISTimedActionQueue.add(ISInventoryTransferAction:new(player, item, container, needy:getInventory()))
    deps.say(player, donor, "item_ok")
    if Settings.diagnosticsEnabled() then
        print("[PersonalKnox][item-ask-npc] donor=" .. tostring(donor) .. " needy=" .. tostring(needy) .. " kind=" .. kind)
    end
    return true
end

local function poll(player)
    if isClient() or not deps or Perf.skip() or not Settings.npcItemAsks() then return end
    local now = getTimestampMs()
    if now - lastCheck < INTERVAL_MS then return end
    lastCheck = now
    local roster = deps.roster(player)
    if #roster == 0 then return end
    local checked = 0
    for _, npc in ipairs(roster) do
        if checked >= MAX_NPCS then break end
        local body = npc:getWorldObject()
        if body and not body:isDead() and not body:hasTimedActions() then
            checked = checked + 1
            local hunger = body:getStats():get(CharacterStat.HUNGER)
            local thirst = body:getStats():get(CharacterStat.THIRST)
            local kind = hunger >= 0.60 and "food" or (thirst >= 0.60 and "water" or nil)
            if kind then
                for _, donor in ipairs(roster) do
                    if donor ~= npc then
                        local donorBody = donor:getWorldObject()
                        if donorBody and not donorBody:isDead() and not donorBody:hasTimedActions() then
                            local donorNeed = kind == "food"
                                and donorBody:getStats():get(CharacterStat.HUNGER)
                                or donorBody:getStats():get(CharacterStat.THIRST)
                            if donorNeed < 0.40 then
                                local item = findItem(donorBody, kind)
                                if item and share(player, donor, npc, item, kind) then break end
                            end
                        end
                    end
                end
            end
        end
    end
end

function AutoAsk.install(adapters)
    deps = adapters
    Events.OnGameStart.Add(function() lastCheck = 0 end)
    Events.OnPlayerUpdate.Add(function()
        local player = getSpecificPlayer(0)
        if not player or player:isDead() then return end
        local ok, err = pcall(poll, player)
        if not ok then print("[PersonalKnox] Auto-ask error: " .. tostring(err)) end
    end)
end

return AutoAsk
