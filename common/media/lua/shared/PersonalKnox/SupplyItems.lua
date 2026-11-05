-- Read actual properties; Java owns food/water eligibility for stocks and eating.
local Items = {}

function Items.food(body, item, java)
    if not instanceof(item, "Food") then return false, false end
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
