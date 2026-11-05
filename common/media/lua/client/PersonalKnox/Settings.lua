-- Native B42 options; no polling, extra dependency or writes outside Apply.
local Settings = {}
local page
local ok = pcall(require, "PZAPI/ModOptions")
if ok and PZAPI and PZAPI.ModOptions then
    page = PZAPI.ModOptions:getOptions("PersonalKnox")
    if not page then
        page = PZAPI.ModOptions:create("PersonalKnox", getText("UI_PersonalKnox_SettingsTitle"))
        page:addTickBox("speech", getText("UI_PersonalKnox_Speech"), true,
            getText("UI_PersonalKnox_SpeechHint"))
        page:addTickBox("diagnostics", getText("UI_PersonalKnox_Diagnostics"), false,
            getText("UI_PersonalKnox_DiagnosticsHint"))
        function page:apply()
            if PersonalKnoxAPI then PersonalKnoxAPI.getInstance():resetWorldReactions() end
        end
    end
end

local function value(key, fallback)
    local option = page and page:getOption(key)
    if not option then return fallback end
    return option:getValue() == true
end

function Settings.speechEnabled() return value("speech", true) end
function Settings.diagnosticsEnabled() return value("diagnostics", false) end
return Settings
