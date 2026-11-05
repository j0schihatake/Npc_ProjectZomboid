-- Snapshot the package declaration when this Lua environment loads, not on each poll.
local ok, version = pcall(function()
    return getModInfoByID("PersonalKnox"):getModVersion()
end)
return {loaded = ok and type(version)=="string" and version~="" and version or "unverified"}
