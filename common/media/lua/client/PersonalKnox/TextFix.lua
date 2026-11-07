-- Global chat/radio text pass: keep the bitmap font readable.
local TextFix = {}

function TextFix.install()
    Events.OnAddMessage.Add(function(message, tabID)
        if not message or not PersonalKnoxAPI then return end
        local okText, text = pcall(function() return message:getText() end)
        if not okText or type(text) ~= "string" then return end
        local okFix, fixed = pcall(function()
            return PersonalKnoxAPI.getInstance():ownTextForChat(text)
        end)
        if okFix and type(fixed) == "string" and fixed ~= text then
            pcall(function() message:setText(fixed) end)
        end
    end)
end

return TextFix
