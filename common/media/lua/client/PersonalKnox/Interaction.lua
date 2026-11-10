-- Optional UI integration; no target search and no Java patch.
local Interaction = {}
local deps

local function nearTarget(player, npc)
    if isClient() or not classFilesInstalled or not player or player:isDead() or not npc then return false end
    local body = npc:getWorldObject()
    if not body or body:isDead() or math.abs(body:getZ() - player:getZ()) > 0.5 then return false end
    local dx, dy = body:getX() - player:getX(), body:getY() - player:getY()
    return dx * dx + dy * dy <= 100 and player:CanSee(body)
end

local function ownBodyId(npc)
    if not PersonalKnoxAPI or not npc then return "" end
    local ok, body = pcall(function() return npc:getWorldObject() end)
    if not ok or not body then return "" end
    local okId, id = pcall(function() return PersonalKnoxAPI.getInstance():ownBodyId(body) end)
    return okId and id or ""
end

local function eligible(player, npc)
    if not PersonalKnoxAPI or not nearTarget(player, npc) then return false end
    local ok, member = pcall(deps.isMember, player, npc)
    return (ok and member) or ownBodyId(npc) ~= ""
end

local function popup(player)
    local num = player:getPlayerNum()
    local menu = ISContextMenu.get(num,
        getPlayerScreenLeft(num) + getPlayerScreenWidth(num) / 2,
        getPlayerScreenTop(num) + getPlayerScreenHeight(num) / 2)
    ISContextMenu.globalPlayerContext = num
    return menu
end

local function openOwn(menu, player, id)
    local api = PersonalKnoxAPI.getInstance()
    menu:addOption(getText("ContextMenu_PersonalKnox_CheckStatus"), player, function(p)
        p:Say(tostring(api:ownSummary(id)))
    end)
    menu:addOption(getText("ContextMenu_PersonalKnox_Squad"), player, function()
        local panel = require "PersonalKnox/OwnNpcPanel"
        panel.toggle(player)
    end)
    menu:addOption(getText("ContextMenu_PersonalKnox_Base"), player, function()
        local panel = require "PersonalKnox/OwnNpcBasePanel"
        panel.toggle(player)
    end)
    menu:addOption(getText("ContextMenu_PersonalKnox_Follow"), player, function(p)
        api:ownOrderMode(id, "FOLLOW", p:getX(), p:getY())
    end)
    menu:addOption(getText("ContextMenu_PersonalKnox_Hold"), player, function(p)
        api:ownOrderMode(id, "HOLD", p:getX(), p:getY())
    end)
end

function Interaction.open(player, npc)
    -- Recheck on selection: the target can leave or die while a menu is open.
    if not eligible(player, npc) then return end
    local menu = popup(player)
    local id = ownBodyId(npc)
    if id ~= "" then
        openOwn(menu, player, id)
    else
        deps.reactions(menu, player)
        deps.fill(menu, player, npc)
    end
end

local function nativeActions(player, npc)
    local data = getPlayerData and getPlayerData(player:getPlayerNum())
    local panel = data and data.npcContextualActionListUI
    if not panel or not NpcContextualActionListUI or not NpcContextualActionListUI.buildNpcActions then return nil end
    return NpcContextualActionListUI.buildNpcActions(panel, npc)
end

local function selectNative(player, npc, label)
    if not nearTarget(player, npc) then return end
    -- Rebuild permissions: a former companion must not retain an inventory button.
    for _, action in ipairs(nativeActions(player, npc) or {}) do
        if action.label == label and action.onSelect and not action.navigation then
            action.onSelect()
            return
        end
    end
end

local function addonOptions(player, npc)
    local options = {}
    local collector = {}
    function collector:addOption(label, target, fn, ...)
        if not fn then return end
        options[#options + 1] = {label=label, target=target, fn=fn, args={...}, n=select("#", ...)}
    end
    deps.reactions(collector, player)
    deps.fill(collector, player, npc)
    return options
end

local function selectAddon(player, npc, label)
    if not eligible(player, npc) then return end
    for _, option in ipairs(addonOptions(player, npc)) do
        if option.label == label then
            option.fn(option.target, unpack(option.args, 1, option.n))
            return
        end
    end
end

function Interaction.attach()
    if NpcContextualActionListUI and not Interaction.knoxAttached then
        local original = NpcContextualActionListUI.buildNpcActions
        if type(original) == "function" then
            NpcContextualActionListUI.buildNpcActions = function(panel, npc)
                local actions = original(panel, npc)
                local player = getSpecificPlayer(panel.playerNum)
                if eligible(player, npc) then
                    table.insert(actions, 1, {
                        label = getText("ContextMenu_PersonalKnox_Squad"), npc = npc,
                        onSelect = function() Interaction.open(player, npc) end,
                    })
                end
                return actions
            end
            Interaction.knoxAttached = true
        end
    end
    if ViewpointInteract and type(ViewpointInteract.harvest) == "function" and not Interaction.viewpointAttached then
        local original = ViewpointInteract.harvest
        ViewpointInteract.harvest = function(player, object)
            local result = original(player, object)
            if not result or not result.labels or not result.enabled or not instanceof(object, "IsoNpcPlayer") then return result end
            local npc = object:getMetaCharacter()
            local function add(group, label, callback)
                local name = getText("ContextMenu_PersonalKnox_ActionLabel", group, label)
                table.insert(ViewpointInteract.actions, {
                    name = name, fn = callback, args = {player, npc, label}, n = 3, enabled = true,
                })
                table.insert(result.labels, name)
                table.insert(result.enabled, true)
            end
            if nearTarget(player, npc) then
                local squadLabel = getText("ContextMenu_PersonalKnox_Squad")
                for _, action in ipairs(nativeActions(player, npc) or {}) do
                    if action.onSelect and not action.navigation and action.label ~= squadLabel then
                        add("Knox", action.label, selectNative)
                    end
                end
                if eligible(player, npc) then
                    for _, option in ipairs(addonOptions(player, npc)) do
                        add(squadLabel, option.label, selectAddon)
                    end
                end
            end
            return result
        end
        Interaction.viewpointAttached = true
    end
end

function Interaction.install(adapters)
    deps = adapters
    Events.OnGameStart.Add(Interaction.attach)
end
return Interaction
