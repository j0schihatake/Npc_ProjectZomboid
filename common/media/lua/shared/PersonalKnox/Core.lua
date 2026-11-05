-- Thin Lua bridge; decisions and dialogue selection live in Java.
local Core = {}
local function java()
    assert(PersonalKnoxAPI, "Approve PersonalKnox.jar in ZombieBuddy before using this addon")
    return PersonalKnoxAPI.getInstance()
end
function Core.pickDialogue(_, topic, state, roll)
    if not PersonalKnoxAPI then return nil end
    local id = java():chooseDialogue(topic, state.morale or 0.5,
        state.relationship or 50, state.bravery or 50, roll or 0)
    if id < 0 then return nil end
    return {id=id, text=java():dialogueText(id)}
end
function Core.appendJournal(journal, entry, limit)
    journal[#journal + 1] = entry
    while #journal > (limit or 100) do table.remove(journal, 1) end
end
function Core.distanceSquared(a, b)
    return (a.x-b.x)^2 + (a.y-b.y)^2
end
function Core.classifyTarget(a, b, range)
    return java():targetLevel(a.x,a.y,a.z,b.x,b.y,b.z,range)
end
function Core.supplyPlan(food, water, medicine, weapon, danger, overloaded)
    return java():supplyPlan(food,water,medicine,weapon,danger,overloaded)
end
return Core
