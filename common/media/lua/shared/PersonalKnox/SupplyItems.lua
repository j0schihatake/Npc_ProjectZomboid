-- Read actual properties; Java owns food/water eligibility for stocks and eating.
local Items = {}

local function hasOpener(body)
    local items = body:getInventory():getItems()
    for i = 0, items:size() - 1 do
        local kind = (items:get(i):getType() or ""):lower()
        if kind:find("canopener", 1, true) or kind:find("tinopener", 1, true)
                or kind:find("bottleopener", 1, true) then return true end
    end
    return false
end

function Items.food(body, item, java)
    if not instanceof(item, "Food") then return false, false end
    local eatType = item:getEatType()
    if (eatType == "Can" or eatType == "Candrink") and not hasOpener(body) then
        return false, false
    end
    local ok, ready = pcall(function()
        return java:isSupplyFood(item:getHungerChange(), not item:getScriptItem():isCantEat(),
            item:isRotten(), item:isBurnt(), item:isbDangerousUncooked(), item:isCooked(),
            body:isKnownPoison(item))
    end)
    return ok and ready == true, not ok
end

function Items.water(item, java)
    local ok, ready = pcall(function()
        if not item:isWaterSource() then return false end
        local fluid = item:getFluidContainer()
        if not fluid then return false end
        return java:isSupplyWater(fluid:getAmount(), fluid:isPureFluid(Fluid.Water), fluid:isTainted())
    end)
    return ok and ready == true, not ok
end

return Items
