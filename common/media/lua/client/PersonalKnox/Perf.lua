local Perf = {}

-- Background chatter/scan work must not add load when the game is already GPU/CPU-bound.
function Perf.lowFps()
    local ok, fps = pcall(function() return getAverageFPS() end)
    return ok and type(fps) == "number" and fps > 0 and fps < 25
end

return Perf
