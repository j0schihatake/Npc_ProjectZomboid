-- Minimal squad window: one row per own NPC with basic orders.
local ISCollapsableWindow = ISCollapsableWindow
local ISButton = ISButton
local ISLabel = ISLabel

local OwnNpcPanel = ISCollapsableWindow:derive("OwnNpcPanel")
local instance = nil

local function api()
    return PersonalKnoxAPI and PersonalKnoxAPI.getInstance() or nil
end

function OwnNpcPanel:createChildren()
    ISCollapsableWindow.createChildren(self)
    self:addChild(ISLabel:new(10, 8, 20, "Squad", 1, 1, 1, 1, UIFont.Medium, false))
    local base = ISButton:new(self.width - 80, 8, 64, 20, "Base", self, function()
        local panel = require "PersonalKnox/OwnNpcBasePanel"
        panel.toggle(self.player)
    end)
    base:initialise(); self:addChild(base)
    local a = api()
    if not a then return end
    self.rowLabels = {}
    local y = 36
    for i = 0, a:ownCount() - 1 do
        local id = a:ownId(i)
        if id and id ~= "" then
            local identity = tostring(a:ownIdentity(id))
            if identity == "" then identity = id end
            local label = ISLabel:new(10, y, 20, identity, 1, 1, 1, 1, UIFont.Small, false)
            self:addChild(label)
            self.rowLabels[id] = label
            local x = self.width - 300
            local follow = ISButton:new(x, y - 2, 64, 20, "Follow", self, function() a:ownCommand(id, "FOLLOW", self.player:getX(), self.player:getY(), self.player:getZ()) end)
            follow:initialise(); self:addChild(follow)
            local hold = ISButton:new(x + 68, y - 2, 60, 20, "Hold", self, function() a:ownCommand(id, "HOLD", self.player:getX(), self.player:getY(), self.player:getZ()) end)
            hold:initialise(); self:addChild(hold)
            local guard = ISButton:new(x + 132, y - 2, 68, 20, "Guard", self, function() a:ownCommand(id, "GUARD", self.player:getX(), self.player:getY(), self.player:getZ()) end)
            guard:initialise(); self:addChild(guard)
            local loot = ISButton:new(x + 204, y - 2, 48, 20, "Loot", self, function() a:ownCommand(id, "LOOT", self.player:getX(), self.player:getY(), self.player:getZ()) end)
            loot:initialise(); self:addChild(loot)
            local rest = ISButton:new(x + 254, y - 2, 48, 20, "Rest", self, function() a:ownCommand(id, "REST", self.player:getX(), self.player:getY(), self.player:getZ()) end)
            rest:initialise(); self:addChild(rest)
            y = y + 24
        end
    end
    self:setHeight(math.max(80, y + 8))
end

function OwnNpcPanel:prerender()
    ISCollapsableWindow.prerender(self)
end

function OwnNpcPanel:render()
    ISCollapsableWindow.render(self)
end

function OwnNpcPanel:update()
    if not self.visibilityLogged then
        self.visibilityLogged = true
        print("[PersonalKnox][own-panel] update visible=" .. tostring(self:getIsVisible()))
    end
    ISCollapsableWindow.update(self)
    local now = getTimestampMs()
    if self.lastRefresh and now - self.lastRefresh < 1000 then return end
    self.lastRefresh = now
    local a = api()
    if not a or not self.rowLabels then return end
    for id, label in pairs(self.rowLabels) do
        local identity = a:ownIdentity(id)
        if identity and identity ~= "" then label:setName(identity) end
    end
end

function OwnNpcPanel:close()
    self:setVisible(false)
    self:removeFromUIManager()
    if instance == self then instance = nil end
end

function OwnNpcPanel.closeOpen()
    if instance then instance:close() end
end

function OwnNpcPanel.isOpen()
    return instance ~= nil and instance:getIsVisible()
end

function OwnNpcPanel.toggle(player)
    if instance then
        instance:close()
        return
    end
    local screenWidth = getCore():getScreenWidth()
    local panel = OwnNpcPanel:new(screenWidth - 780, 100, 740, 120)
    panel.player = player
    panel:setTitle("Squad")
    panel:setResizable(false)
    panel:initialise()
    panel:setVisible(true)
    panel:addToUIManager()
    panel:setAlwaysOnTop(true)
    panel:bringToTop()
    instance = panel
    print("[PersonalKnox][own-panel] java=" .. tostring(panel.javaObject ~= nil)
        .. " opened x=" .. tostring(panel:getX())
        .. " y=" .. tostring(panel:getY())
        .. " w=" .. tostring(panel:getWidth())
        .. " h=" .. tostring(panel:getHeight()))
end

return OwnNpcPanel
