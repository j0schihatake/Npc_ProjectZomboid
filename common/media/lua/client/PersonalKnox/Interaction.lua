-- Optional UI integration; no target search and no Java patch.
local Interaction = {}
local deps

local function eligible(player, npc)
    if isClient() or not classFilesInstalled or not PersonalKnoxAPI or not player or
        player:isDead() or not npc or not deps.isMember(player, npc) then return false end
    local body = npc:getWorldObject()
    if not body or body:isDead() or math.abs(body:getZ() - player:getZ()) > 0.5 then return false end
    local dx, dy = body:getX() - player:getX(), body:getY() - player:getY()
    return dx * dx + dy * dy <= 100 and player:CanSee(body)
end

function Interaction.open(player, npc)
    -- Recheck on selection: the target can leave or die while a menu is open.
    if not eligible(player, npc) then return end
    local num = player:getPlayerNum()
    local menu = ISContextMenu.get(num,
        getPlayerScreenLeft(num) + getPlayerScreenWidth(num) / 2,
        getPlayerScreenTop(num) + getPlayerScreenHeight(num) / 2)
    ISContextMenu.globalPlayerContext = num
    deps.reactions(menu, player)
    deps.fill(menu, player, npc)
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
            if eligible(player, npc) then
                local label = getText("ContextMenu_PersonalKnox_Squad")
                table.insert(ViewpointInteract.actions, {
                    name = label, fn = Interaction.open, args = {player, npc}, n = 2, enabled = true,
                })
                table.insert(result.labels, label)
                table.insert(result.enabled, true)
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
