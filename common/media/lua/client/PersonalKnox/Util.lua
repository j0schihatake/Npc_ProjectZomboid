local Util = {}

-- Prefer getId when available (tests/older objects), then getUsername, then the object.
function Util.id(npc)
    if not npc then return "none" end
    local ok, id = pcall(function() return npc:getId() end)
    if ok and id ~= nil and id ~= "" then return tostring(id) end
    local ok2, name = pcall(function() return npc:getUsername() end)
    if ok2 and name ~= nil and name ~= "" then return tostring(name) end
    return tostring(npc)
end

return Util
