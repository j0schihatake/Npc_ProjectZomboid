-- Minimal squad window: one row per own NPC with basic orders.
local ISPanel = ISPanel
local ISButton = ISButton
local ISLabel = ISLabel

local OwnNpcPanel = ISPanel:derive("OwnNpcPanel")
local instance = nil

local function api()
    return PersonalKnoxAPI and PersonalKnoxAPI.getInstance() or nil
end

function OwnNpcPanel:createChildren()
    ISPanel.createChildren(self)
    self:addChild(ISLabel:new(10, 8, 20, "Свой отряд", 1, 1, 1, 1, UIFont.Medium, false))
    local close = ISButton:new(self.width - 30, 6, 24, 22, "X", self, OwnNpcPanel.close)
    close:initialise()
    self:addChild(close)
    local a = api()
    if not a then return end
    self.rowLabels = {}
    local y = 36
    for i = 0, a:ownCount() - 1 do
        local id = a:ownId(i)
        if id and id ~= "" then
            local label = ISLabel:new(10, y, 20, tostring(a:ownSummary(id)), 1, 1, 1, 1, UIFont.Small, false)
            self:addChild(label)
            self.rowLabels[id] = label
            local x = self.width - 210
            local follow = ISButton:new(x, y - 2, 64, 20, "За мной", self, function() a:ownCommand(id, "FOLLOW", self.player:getX(), self.player:getY(), self.player:getZ()) end)
            follow:initialise(); self:addChild(follow)
            local hold = ISButton:new(x + 68, y - 2, 60, 20, "Стоять", self, function() a:ownCommand(id, "HOLD", self.player:getX(), self.player:getY(), self.player:getZ()) end)
            hold:initialise(); self:addChild(hold)
            local guard = ISButton:new(x + 132, y - 2, 68, 20, "Охрана", self, function() a:ownCommand(id, "GUARD", self.player:getX(), self.player:getY(), self.player:getZ()) end)
            guard:initialise(); self:addChild(guard)
            local loot = ISButton:new(x + 204, y - 2, 52, 20, "Сбор", self, function() a:ownCommand(id, "LOOT", self.player:getX(), self.player:getY(), self.player:getZ()) end)
            loot:initialise(); self:addChild(loot)
            y = y + 24
        end
    end
    self:setHeight(math.max(80, y + 8))
end

function OwnNpcPanel:update()
    ISPanel.update(self)
    local now = getTimestampMs()
    if self.lastRefresh and now - self.lastRefresh < 1000 then return end
    self.lastRefresh = now
    local a = api()
    if not a or not self.rowLabels then return end
    for id, label in pairs(self.rowLabels) do
        local summary = a:ownSummary(id)
        if summary and summary ~= "" then label:setName(summary) end
    end
end

function OwnNpcPanel:close()
    self:setVisible(false)
    self:removeFromUIManager()
    if instance == self then instance = nil end
end

function OwnNpcPanel.toggle(player)
    if instance then
        instance:close()
        return
    end
    local panel = OwnNpcPanel:new(80, 80, 560, 120)
    panel.player = player
    panel:initialise()
    panel:addToUIManager()
    panel:setVisible(true)
    panel:setAlwaysOnTop(true)
    panel:bringToTop()
    instance = panel
    print("[PersonalKnox][own-panel] opened x=" .. tostring(panel:getX())
        .. " y=" .. tostring(panel:getY())
        .. " w=" .. tostring(panel:getWidth())
        .. " h=" .. tostring(panel:getHeight()))
end

return OwnNpcPanel
