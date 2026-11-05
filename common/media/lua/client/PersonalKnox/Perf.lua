local Perf = {}
local Settings = require "PersonalKnox/Settings"

-- Background chatter/scan work must not add load when the game is already GPU/CPU-bound.
function Perf.lowFps()
    local ok, fps = pcall(function() return getAverageFPS() end)
    return ok and type(fps) == "number" and fps > 0 and fps < 25
end

function Perf.skip()
    return Settings.performanceMode() or Perf.lowFps()
end

function Perf.install()
    local last = 0
    Events.OnPlayerUpdate.Add(function()
        local player = getSpecificPlayer(0)
        if not player then return end
        local now = getTimestampMs()
        if now - last < 60000 then return end
        last = now
        if Settings.diagnosticsEnabled() then
            local ok, fps = pcall(function() return getAverageFPS() end)
            print("[PersonalKnox][perf] fps=" .. tostring(ok and fps or "?")
                .. " performance=" .. tostring(Settings.performanceMode()))
        end
    end)
end

return Perf
