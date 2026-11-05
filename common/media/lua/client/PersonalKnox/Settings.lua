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
    ensure("autoUnstick", "UI_PersonalKnox_AutoUnstick", true, "UI_PersonalKnox_AutoUnstickHint")
    ensure("careSearchBags", "UI_PersonalKnox_CareSearchBags", true, "UI_PersonalKnox_CareSearchBagsHint")
    ensure("npcSearchContainers", "UI_PersonalKnox_NpcSearchContainers", true, "UI_PersonalKnox_NpcSearchContainersHint")
    ensure("npcBandageStock", "UI_PersonalKnox_NpcBandageStock", true, "UI_PersonalKnox_NpcBandageStockHint")
    ensure("npcStock", "UI_PersonalKnox_NpcStock", true, "UI_PersonalKnox_NpcStockHint")
    ensure("npcVoices", "UI_PersonalKnox_NpcVoices", true, "UI_PersonalKnox_NpcVoicesHint")
    ensure("performanceMode", "UI_PersonalKnox_PerformanceMode", false, "UI_PersonalKnox_PerformanceModeHint")
    ensure("npcItemAsks", "UI_PersonalKnox_NpcItemAsks", true, "UI_PersonalKnox_NpcItemAsksHint")
    if page and not page:getOption("ammoMode") then
        local combo = page:addComboBox("ammoMode", getText("UI_PersonalKnox_AmmoMode"),
            getText("UI_PersonalKnox_AmmoModeHint"))
        combo:addItem("UI_PersonalKnox_AmmoNormal")
        combo:addItem("UI_PersonalKnox_AmmoInfiniteReserve", true)
        combo:addItem("UI_PersonalKnox_AmmoInfiniteMagazine")
        combo:addItem("UI_PersonalKnox_AmmoInfiniteReload")
    end
    if page and not page:getOption("reloadMode") then
        local combo = page:addComboBox("reloadMode", getText("UI_PersonalKnox_ReloadMode"),
            getText("UI_PersonalKnox_ReloadModeHint"))
        combo:addItem("UI_PersonalKnox_ReloadStandard")
        combo:addItem("UI_PersonalKnox_ReloadAuto", true)
        combo:addItem("UI_PersonalKnox_ReloadNever")
    end
    if page and not page:getOption("npcNeedRate") then
        local combo = page:addComboBox("npcNeedRate", getText("UI_PersonalKnox_NpcNeedRate"),
            getText("UI_PersonalKnox_NpcNeedRateHint"))
        combo:addItem("UI_PersonalKnox_NeedVerySlow")
        combo:addItem("UI_PersonalKnox_NeedSlow")
        combo:addItem("UI_PersonalKnox_NeedNormal", true)
        combo:addItem("UI_PersonalKnox_NeedFast")
        combo:addItem("UI_PersonalKnox_NeedVeryFast")
    end
    if page and not page:getOption("chatterFrequency") then
        local combo = page:addComboBox("chatterFrequency", getText("UI_PersonalKnox_ChatterFrequency"),
            getText("UI_PersonalKnox_ChatterFrequencyHint"))
        combo:addItem("UI_PersonalKnox_ChatterOff")
        combo:addItem("UI_PersonalKnox_ChatterRare")
        combo:addItem("UI_PersonalKnox_ChatterNormal", true)
        combo:addItem("UI_PersonalKnox_ChatterOften")
    end
    if page and not page:getOption("helpFrequency") then
        local combo = page:addComboBox("helpFrequency", getText("UI_PersonalKnox_HelpFrequency"),
            getText("UI_PersonalKnox_HelpFrequencyHint"))
        combo:addItem("UI_PersonalKnox_HelpOff")
        combo:addItem("UI_PersonalKnox_HelpRare")
        combo:addItem("UI_PersonalKnox_HelpNormal", true)
        combo:addItem("UI_PersonalKnox_HelpOften")
    end
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
function Settings.ammoMode()
    local option = page and page:getOption("ammoMode")
    if not option then return 2 end
    local choice = option:getValue()
    return type(choice) == "number" and choice or 2
end
function Settings.infiniteAmmo() return Settings.ammoMode() >= 2 end
function Settings.infiniteMagazine() return Settings.ammoMode() == 3 end
function Settings.reloadOnlyWhenEmpty() return Settings.ammoMode() == 4 end
function Settings.autoUnstick() return value("autoUnstick", true) end
function Settings.careSearchBags() return value("careSearchBags", true) end
function Settings.npcVoices() return value("npcVoices", true) end
function Settings.reloadMode()
    local option = page and page:getOption("reloadMode")
    if not option then return 2 end
    local choice = option:getValue()
    return type(choice) == "number" and choice or 2
end
function Settings.npcSearchContainers() return value("npcSearchContainers", true) end
function Settings.npcStock() return value("npcStock", true) end
function Settings.npcBandageStock() return value("npcBandageStock", true) end
function Settings.autoReload() return Settings.reloadMode() == 2 end
function Settings.npcItemAsks() return value("npcItemAsks", true) end
function Settings.performanceMode() return value("performanceMode", false) end
function Settings.chatterFrequency()
    local option = page and page:getOption("chatterFrequency")
    if not option then return 3 end
    local choice = option:getValue()
    return type(choice) == "number" and choice or 3
end
function Settings.npcNeedRate()
    local option = page and page:getOption("npcNeedRate")
    if not option then return 3 end
    local choice = option:getValue()
    return type(choice) == "number" and choice or 3
end
function Settings.helpFrequency()
    local option = page and page:getOption("helpFrequency")
    if not option then return 3 end
    local choice = option:getValue()
    return type(choice) == "number" and choice or 3
end
return Settings
