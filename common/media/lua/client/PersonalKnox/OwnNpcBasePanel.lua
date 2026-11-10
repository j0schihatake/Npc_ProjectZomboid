-- Base roster window: names, current professions and random multi-profession assignment.
local ISCollapsableWindow = ISCollapsableWindow
local ISButton = ISButton
local ISLabel = ISLabel
local OwnNpcBasePanel = ISCollapsableWindow:derive("OwnNpcBasePanel")
local instance = nil
local TAGS = {"DOCTOR", "CARPENTER", "FARMER", "MECHANIC", "POLICE", "VETERAN", "CHEF", "FISHER", "GUARD"}
local randomTag = {}

local function api()
    return PersonalKnoxAPI and PersonalKnoxAPI.getInstance() or nil
end

local function randomCsv()
    local picked = {}
    for _, tag in ipairs(TAGS) do
        if randomTag[tag] then picked[#picked + 1] = tag end
    end
    if #picked == 0 then picked[1] = TAGS[math.random(#TAGS)] end
    for i = #picked, 2, -1 do
        local j = math.random(i)
        picked[i], picked[j] = picked[j], picked[i]
    end
    local count = math.random(1, math.min(#picked, 3))
    local out = {}
    for i = 1, count do out[i] = picked[i] end
    return table.concat(out, ",")
end

function OwnNpcBasePanel:createChildren()
    ISCollapsableWindow.createChildren(self)
    self.rowLabels = {}
    self.profButtons = {}
    self.selected = {}
    self:addChild(ISLabel:new(10, 8, 20, "Base roster", 1, 1, 1, 1, UIFont.Medium, false))
    local x, y = 10, 32
    for _, tag in ipairs(TAGS) do
        local button = ISButton:new(x, y, 92, 22, tag, self, function() self:toggleTag(tag) end)
        button:initialise(); self:addChild(button)
        self.profButtons[tag] = button
        x = x + 96
        if x > self.width - 96 then x = 10; y = y + 26 end
    end
    local assignAll = ISButton:new(10, y + 30, 180, 24, "Assign random to all", self, function() self:assignAll() end)
    assignAll:initialise(); self:addChild(assignAll)
    self.listY = y + 64
    self:rebuildRows()
end

function OwnNpcBasePanel:rebuildRows()
    for _, label in pairs(self.rowLabels) do self:removeChild(label) end
    self.rowLabels = {}
    local a = api()
    if not a then return end
    local y = self.listY
    for i = 0, a:ownCount() - 1 do
        local id = a:ownId(i)
        if id and id ~= "" then
            local label = ISLabel:new(10, y, 20, tostring(a:ownIdentity(id)),
                1, 1, 1, 1, UIFont.Small, false)
            self:addChild(label)
            self.rowLabels[id] = label
            local random = ISButton:new(self.width - 100, y - 2, 82, 20, "Random", self,
                function() self:assignOne(id) end)
            random:initialise(); self:addChild(random)
            y = y + 24
        end
    end
    self:setHeight(math.max(260, y + 12))
end

function OwnNpcBasePanel:toggleTag(tag)
    self.selected[tag] = not self.selected[tag]
    local button = self.profButtons[tag]
    if button then button:setTitle((self.selected[tag] and "[x] " or "[ ] ") .. tag) end
end

function OwnNpcBasePanel:assignOne(id)
    local a = api()
    if not a then return end
    for _, tag in ipairs(TAGS) do randomTag[tag] = self.selected[tag] or false end
    a:ownAssignProfessions(id, randomCsv())
    local label = self.rowLabels[id]
    if label then label:setName(tostring(a:ownIdentity(id)) .. " | " .. tostring(a:ownProfessions(id))) end
end

function OwnNpcBasePanel:assignAll()
    local a = api()
    if not a then return end
    for i = 0, a:ownCount() - 1 do self:assignOne(a:ownId(i)) end
end

function OwnNpcBasePanel:update()
    ISCollapsableWindow.update(self)
end

function OwnNpcBasePanel:close()
    self:setVisible(false)
    self:removeFromUIManager()
    if instance == self then instance = nil end
end

function OwnNpcBasePanel.closeOpen()
    if instance then instance:close() end
end

function OwnNpcBasePanel.isOpen()
    return instance ~= nil and instance:getIsVisible()
end

function OwnNpcBasePanel.toggle(player)
    if instance then instance:close(); return end
    local panel = OwnNpcBasePanel:new(getCore():getScreenWidth() - 780, 100, 740, 360)
    panel.player = player
    panel:setTitle("Base roster")
    panel:setResizable(false)
    panel:initialise()
    panel:setVisible(true)
    panel:addToUIManager()
    panel:setAlwaysOnTop(true)
    panel:bringToTop()
    instance = panel
    print("[PersonalKnox][base-panel] opened")
end

return OwnNpcBasePanel
