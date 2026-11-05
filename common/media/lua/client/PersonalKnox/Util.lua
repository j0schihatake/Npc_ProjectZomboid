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

-- B42 removed getItemCountFromTypeRecursive on some builds; fall back to a bounded scan.
function Util.countAmmo(inventory, itemKey)
    if not inventory or not itemKey then return 0 end
    local ok, count = pcall(function() return inventory:getItemCountFromTypeRecursive(itemKey) end)
    if ok and type(count) == "number" then return count end
    local total, items = 0, inventory:getItems()
    for i = 0, items:size() - 1 do
        local item = items:get(i)
        local full = item:getFullType()
        if full == itemKey or item:getType() == itemKey then total = total + item:getCount() end
    end
    return total
end
