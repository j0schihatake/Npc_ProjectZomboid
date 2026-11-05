-- Ask the squad before taking food, water, medicine or ammo.
local Ask = {}
local Settings = require "PersonalKnox/Settings"
local deps = nil

local function kindOf(item)
    if item:IsFood() then return 0 end
    local script = item:getScriptItem()
    if script and script:getThirstChange() < 0 then return 1 end
    if (item:getCategory() or ""):lower() == "medical" then return 2 end
    if item:getAmmoType() then return 3 end
    return 4
end

local function ammoCount(body)
    local count, items = 0, body:getInventory():getItems()
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        if item:getAmmoType() then count = count + item:getCount() end
    end
    return count
end

local function ask(player, item, kind)
    if isClient() or not PersonalKnoxAPI or not item then return end
    local api = PersonalKnoxAPI.getInstance()
    if not api or not deps then return end
    local best, bestWeight, speaker = nil, 0, nil
    local checked = 0
    for _, npc in ipairs(deps.roster(player)) do
        if checked >= 6 then break end
        local body = npc:getWorldObject()
        if body and not body:isDead() then
            local dx, dy = body:getX() - player:getX(), body:getY() - player:getY()
            if dx * dx + dy * dy <= 225 then
                checked = checked + 1
                speaker = speaker or npc
                local inv = body:getInventory()
                local slots = math.max(0, inv:getEffectiveCapacity(body) - inv:getItems():size())
                local hunger = math.max(0, math.min(1, body:getStats():get(CharacterStat.HUNGER)))
                local thirst = math.max(0, math.min(1, body:getStats():get(CharacterStat.THIRST)))
                local health = body:getBodyDamage():getHealth()
                local ok, weight = pcall(function()
                    return api:itemObjection(kind, hunger, thirst, health, slots, ammoCount(body))
                end)
                if ok and weight and weight > bestWeight then best, bestWeight = npc, weight end
            end
        end
    end
    if not speaker then return end
    if best and bestWeight > 0 then
        deps.say(player, best, "item_object")
        local container = item:getContainer()
        if container and ISInventoryTransferAction and ISTimedActionQueue then
            ISTimedActionQueue.add(ISInventoryTransferAction:new(player, item, container, best:getInventory()))
        end
        if Settings.diagnosticsEnabled() then
            print("[PersonalKnox][item-ask] objector=" .. tostring(best:getId())
                .. " weight=" .. string.format("%.2f", bestWeight))
        end
    else
        deps.say(player, speaker, "item_ok")
        if Settings.diagnosticsEnabled() then
            print("[PersonalKnox][item-ask] ok npc=" .. tostring(speaker:getId()))
        end
    end
end

local function addOption(player, context, items)
    if #items ~= 1 then return end
    local item = items[1]
    if not item or not instanceof(item, "InventoryItem") then return end
    context:addOption(getText("ContextMenu_PersonalKnox_AskTake"), player, ask, item, kindOf(item))
end

function Ask.install(adapters)
    deps = adapters
    Events.OnFillInventoryObjectContextMenu.Add(addOption)
end

return Ask
