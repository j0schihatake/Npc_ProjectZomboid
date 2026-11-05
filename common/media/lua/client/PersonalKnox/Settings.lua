-- Native B42 options; no polling, extra dependency or writes outside Apply.
local Settings = {}
local page
local ok = pcall(require, "PZAPI/ModOptions")
if ok and PZAPI and PZAPI.ModOptions then
    page = PZAPI.ModOptions:getOptions("PersonalKnox")
    if not page then
        page = PZAPI.ModOptions:create("PersonalKnox", getText("UI_PersonalKnox_SettingsTitle"))
        function page:apply()
            if PersonalKnoxAPI then PersonalKnoxAPI.getInstance():resetWorldReactions() end
        end
    end
    -- Existing pages receive the new toggles without losing saved values.
    local function ensure(key, label, default, hint)
        if not page:getOption(key) then
            page:addTickBox(key, getText(label), default, getText(hint))
        end
    end
    ensure("speech", "UI_PersonalKnox_Speech", true, "UI_PersonalKnox_SpeechHint")
    ensure("diagnostics", "UI_PersonalKnox_Diagnostics", false, "UI_PersonalKnox_DiagnosticsHint")
    ensure("autoMedic", "UI_PersonalKnox_AutoMedic", true, "UI_PersonalKnox_AutoMedicHint")
    ensure("autoDrink", "UI_PersonalKnox_AutoDrink", true, "UI_PersonalKnox_AutoDrinkHint")
    ensure("autoEat", "UI_PersonalKnox_AutoEat", true, "UI_PersonalKnox_AutoEatHint")
    ensure("infiniteDurability", "UI_PersonalKnox_InfiniteDurability", true, "UI_PersonalKnox_InfiniteDurabilityHint")
    ensure("infiniteAmmo", "UI_PersonalKnox_InfiniteAmmo", true, "UI_PersonalKnox_InfiniteAmmoHint")
    ensure("autoUnstick", "UI_PersonalKnox_AutoUnstick", true, "UI_PersonalKnox_AutoUnstickHint")
end

local function value(key, fallback)
    local option = page and page:getOption(key)
    if not option then return fallback end
    return option:getValue() == true
end

function Settings.speechEnabled() return value("speech", true) end
function Settings.diagnosticsEnabled() return value("diagnostics", false) end
function Settings.autoMedic() return value("autoMedic", true) end
function Settings.autoDrink() return value("autoDrink", true) end
function Settings.autoEat() return value("autoEat", true) end
function Settings.infiniteDurability() return value("infiniteDurability", true) end
function Settings.infiniteAmmo() return value("infiniteAmmo", true) end
function Settings.autoUnstick() return value("autoUnstick", true) end
return Settings
