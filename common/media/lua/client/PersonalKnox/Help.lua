-- Frequency gate for future help situations. Scenarios call Help.allowed(now).
local Help = {}
local Settings = require "PersonalKnox/Settings"
local lastAt = 0

function Help.allowed(now)
    if not PersonalKnoxAPI then return false end
    local api = PersonalKnoxAPI.getInstance()
    local frequency = Settings.helpFrequency()
    if api:helpAllowed(frequency, now, lastAt) then
        lastAt = now
        return true
    end
    return false
end

function Help.install()
    Events.OnGameStart.Add(function()
        lastAt = 0
        if Settings.diagnosticsEnabled() then
            print("[PersonalKnox][help] frequency=" .. tostring(Settings.helpFrequency())
                .. " interval_ms=" .. tostring(PersonalKnoxAPI and PersonalKnoxAPI.getInstance():helpInterval(Settings.helpFrequency()) or 0))
        end
    end)
end

return Help
