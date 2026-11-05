-- Reuse Knox's squad-wide weapon preferences; do not replace equipment/combat logic.
local Weapons = {}
local deps
local options = {
    {"ContextMenu_PersonalKnox_PrefMelee", "onWeaponPrefMelee"},
    {"ContextMenu_PersonalKnox_PrefRanged", "onWeaponPrefRanged"},
    {"ContextMenu_PersonalKnox_PrefAny", "onWeaponPrefAny"},
}

local function apply(player, npc, method)
    if isClient() or not player or player:isDead() or not deps.isMember(player, npc) then return end
    local native = ISNpcGroupRadialMenu
    if native and type(native[method]) == "function" then native[method](player) end
end

function Weapons.addOptions(menu, player, npc)
    if not ISNpcGroupRadialMenu then return end
    for _, option in ipairs(options) do
        if type(ISNpcGroupRadialMenu[option[2]]) == "function" then
            menu:addOption(getText(option[1]), player, apply, npc, option[2])
        end
    end
end

function Weapons.install(adapters) deps = adapters end
return Weapons
