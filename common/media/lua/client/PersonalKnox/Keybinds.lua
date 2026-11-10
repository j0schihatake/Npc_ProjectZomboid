-- Configurable window keys; defaults are keys not used by vanilla or debug shortcuts.
local Keybinds = {}
local Settings = require "PersonalKnox/Settings"

function Keybinds.install()
    Events.OnKeyPressed.Add(function(key)
        if getCore():isDoingTextEntry() then return end
        local player = getSpecificPlayer(0)
        if not player or player:isDead() then return end
        if key == Settings.squadKey() then
            local ok, panel = pcall(require, "PersonalKnox/OwnNpcPanel")
            if ok and panel then panel.toggle(player) end
        elseif key == Settings.baseKey() then
            local ok, panel = pcall(require, "PersonalKnox/OwnNpcBasePanel")
            if ok and panel then panel.toggle(player) end
        end
    end)
end

return Keybinds
