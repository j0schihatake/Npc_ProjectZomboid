-- B42 zombie traversal policy: rope allowed/denied/random, crawlers never climb.
local ZombiePath = {}

local function idOf(zombie)
    local ok, id = pcall(function() return zombie:getID() end)
    return ok and tostring(id) or tostring(zombie)
end

local function apply(zombie)
    if not zombie or not PersonalKnoxAPI then return end
    local ok, crawling = pcall(function() return zombie:isCrawling() end)
    local allowed = PersonalKnoxAPI.getInstance():zombiePathAllows(idOf(zombie), ok and crawling == true)
    zombie:getModData().PersonalKnoxRopeAllowed = allowed
end

function ZombiePath.install()
    if not PersonalKnoxAPI then return end
    PersonalKnoxAPI.getInstance():zombiePathMode(
        require("PersonalKnox/Settings").zombiePathMode())
    if Events.OnZombieCreate and Events.OnZombieCreate.Add then
        Events.OnZombieCreate.Add(apply)
    end
    Events.OnZombieUpdate.Add(function(zombie)
        if not zombie then return end
        local ok, rope = pcall(function() return zombie:isClimbingRope() end)
        if not ok or not rope then return end
        apply(zombie)
        local md = zombie:getModData()
        if md.PersonalKnoxRopeAllowed == false then
            pcall(function() zombie:fallFromRope() end)
        end
    end)
end

return ZombiePath
