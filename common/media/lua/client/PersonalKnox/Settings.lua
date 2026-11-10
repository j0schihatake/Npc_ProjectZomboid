-- Native B42 options; no polling, extra dependency or writes outside Apply.
local Settings = {}
local page

local DB_KEYS = {"speech","diagnostics","autoMedic","autoDrink","autoEat","infiniteDurability","autoUnstick","careSearchBags","npcSearchContainers","npcBandageStock","npcStock","npcFinds","npcVoices","performanceMode","worldgenWrites","identityRussianFirst","zombieAttackOwnNpc","npcItemAsks","openSquad","showStartHint","matchPlayerClothes","inGameTests","activeBodyLimit","bodyMoveBudget","bodySyncBudgetMs","worldgenBudget","worldgenProtection","zombiePathMode","ammoMode","reloadMode","ammoWeight","squadWindowKey","baseWindowKey","factionCount","factionMembers","factionSupplies","outpostGarrison","outpostSupplies","factionDistance","factionRanged","factionMelee","companionCount","raidMax","raidFrequency","raidIntervalMinutes","npcNeedRate","helpFrequency"}

local function dbApi()
    return PersonalKnoxAPI and PersonalKnoxAPI.getInstance() or nil
end

local function loadDbSettings()
    local api = dbApi()
    if not api or not page then return end
    local ok, text = pcall(function() return api:dbSettings() end)
    if not ok or not text then return end
    for line in tostring(text):gmatch("[^\n]+") do
        local key, value = line:match("^([^\t]+)\t(.+)$")
        local option = key and page:getOption(key)
        if option then
            local current = option:getValue()
            local parsed = value
            if type(current) == "boolean" then parsed = value == "true"
            elseif type(current) == "number" then parsed = tonumber(value) or current end
            pcall(function() option:setValue(parsed) end)
        end
    end
end

local function saveDbSettings()
    local api = dbApi()
    if not api or not page then return end
    for _, key in ipairs(DB_KEYS) do
        local option = page:getOption(key)
        if option then
            pcall(function() api:dbSetSetting(key, tostring(option:getValue())) end)
        end
    end
end
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
    ensure("npcFinds", "UI_PersonalKnox_NpcFinds", true, "UI_PersonalKnox_NpcFindsHint")
    ensure("npcVoices", "UI_PersonalKnox_NpcVoices", true, "UI_PersonalKnox_NpcVoicesHint")
    ensure("performanceMode", "UI_PersonalKnox_PerformanceMode", false, "UI_PersonalKnox_PerformanceModeHint")
    ensure("worldgenWrites", "UI_PersonalKnox_WorldGenWrites", false, "UI_PersonalKnox_WorldGenWritesHint")
    ensure("identityRussianFirst", "UI_PersonalKnox_IdentityRussianFirst", false, "UI_PersonalKnox_IdentityRussianFirstHint")
    ensure("zombieAttackOwnNpc", "UI_PersonalKnox_ZombieAttackOwnNpc", true, "UI_PersonalKnox_ZombieAttackOwnNpcHint")
    ensure("npcItemAsks", "UI_PersonalKnox_NpcItemAsks", true, "UI_PersonalKnox_NpcItemAsksHint")
    ensure("openSquad", "UI_PersonalKnox_OpenSquad", false, "UI_PersonalKnox_OpenSquadHint")
    if page and not page:getOption("squadWindowKey") then
        page:addKeyBind("squadWindowKey", getText("UI_PersonalKnox_SquadWindowKey"),
            Keyboard.KEY_INSERT, getText("UI_PersonalKnox_SquadWindowKeyHint"))
    end
    if page and not page:getOption("baseWindowKey") then
        page:addKeyBind("baseWindowKey", getText("UI_PersonalKnox_BaseWindowKey"),
            Keyboard.KEY_DELETE, getText("UI_PersonalKnox_BaseWindowKeyHint"))
    end
    ensure("showStartHint", "UI_PersonalKnox_ShowStartHint", true, "UI_PersonalKnox_ShowStartHintHint")
    if page and not page:getOption("activeBodyLimit") then
        page:addSlider("activeBodyLimit", getText("UI_PersonalKnox_ActiveBodyLimit"), 4, 64, 4, 24,
            getText("UI_PersonalKnox_ActiveBodyLimitHint"))
    end
    if page and not page:getOption("bodyMoveBudget") then
        page:addSlider("bodyMoveBudget", getText("UI_PersonalKnox_BodyMoveBudget"), 2, 32, 2, 8,
            getText("UI_PersonalKnox_BodyMoveBudgetHint"))
    end
    if page and not page:getOption("worldgenBudget") then
        page:addSlider("worldgenBudget", getText("UI_PersonalKnox_WorldGenBudget"), 1, 512, 16, 32,
            getText("UI_PersonalKnox_WorldGenBudgetHint"))
    end
    if page and not page:getOption("worldgenProtection") then
        page:addSlider("worldgenProtection", getText("UI_PersonalKnox_WorldGenProtection"), 0, 16, 1, 0,
            getText("UI_PersonalKnox_WorldGenProtectionHint"))
    end
    if page and not page:getOption("bodySyncBudgetMs") then
        page:addSlider("bodySyncBudgetMs", getText("UI_PersonalKnox_BodySyncBudget"), 2, 50, 2, 8,
            getText("UI_PersonalKnox_BodySyncBudgetHint"))
    end
    ensure("matchPlayerClothes", "UI_PersonalKnox_MatchPlayerClothes", false, "UI_PersonalKnox_MatchPlayerClothesHint")
    ensure("inGameTests", "UI_PersonalKnox_InGameTests", false, "UI_PersonalKnox_InGameTestsHint")
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
    if page and not page:getOption("ammoWeight") then
        local combo = page:addComboBox("ammoWeight", getText("UI_PersonalKnox_AmmoWeight"),
            getText("UI_PersonalKnox_AmmoWeightHint"))
        combo:addItem("UI_PersonalKnox_AmmoWeightZero")
        combo:addItem("UI_PersonalKnox_AmmoWeightHalf")
        combo:addItem("UI_PersonalKnox_AmmoWeightFull", true)
    end
    if page and not page:getOption("zombiePathMode") then
        local combo = page:addComboBox("zombiePathMode", getText("UI_PersonalKnox_ZombiePathMode"),
            getText("UI_PersonalKnox_ZombiePathModeHint"))
        combo:addItem("UI_PersonalKnox_ZombiePathNoRope", true)
        combo:addItem("UI_PersonalKnox_ZombiePathWithRope")
        combo:addItem("UI_PersonalKnox_ZombiePathRandom")
    end
    if page and not page:getOption("factionDistance") then
        page:addSlider("factionDistance", getText("UI_PersonalKnox_FactionDistance"), 20, 200, 10, 80,
            getText("UI_PersonalKnox_FactionDistanceHint"))
    end
    if page and not page:getOption("factionCount") then
        local box = page:addComboBox("factionCount", getText("UI_PersonalKnox_FactionCount"),
            getText("UI_PersonalKnox_FactionCountHint"))
        for i = 0, 16 do box:addItem(tostring(i), i == 3) end
    end
    if page and not page:getOption("factionMembers") then
        page:addSlider("factionMembers", getText("UI_PersonalKnox_FactionMembers"), 1, 12, 1, 3,
            getText("UI_PersonalKnox_FactionMembersHint"))
    end
    if page and not page:getOption("factionSupplies") then
        page:addSlider("factionSupplies", getText("UI_PersonalKnox_FactionSupplies"), 0, 10, 1, 2,
            getText("UI_PersonalKnox_FactionSuppliesHint"))
    end
    if page and not page:getOption("outpostGarrison") then
        page:addSlider("outpostGarrison", getText("UI_PersonalKnox_OutpostGarrison"), 0, 6, 1, 1,
            getText("UI_PersonalKnox_OutpostGarrisonHint"))
    end
    if page and not page:getOption("outpostSupplies") then
        page:addSlider("outpostSupplies", getText("UI_PersonalKnox_OutpostSupplies"), 0, 10, 1, 1,
            getText("UI_PersonalKnox_OutpostSuppliesHint"))
    end
    if page and not page:getOption("npcWeapon") then
        local weapon = page:addComboBox("npcWeapon", getText("UI_PersonalKnox_NpcWeapon"),
                {"melee", "firearm", "none"}, getText("UI_PersonalKnox_NpcWeaponHint"))
        weapon:setValue(1)
    end
    if page and not page:getOption("factionRanged") then
        page:addSlider("factionRanged", getText("UI_PersonalKnox_FactionRanged"), 0, 100, 5, 40,
            getText("UI_PersonalKnox_FactionRangedHint"))
    end
    if page and not page:getOption("factionMelee") then
        page:addSlider("factionMelee", getText("UI_PersonalKnox_FactionMelee"), 0, 100, 5, 40,
            getText("UI_PersonalKnox_FactionMeleeHint"))
    end
    if page and not page:getOption("companionCount") then
        local combo = page:addComboBox("companionCount", getText("UI_PersonalKnox_CompanionCount"),
            getText("UI_PersonalKnox_CompanionCountHint"))
        combo:addItem("UI_PersonalKnox_CompanionNone")
        combo:addItem("UI_PersonalKnox_CompanionOne")
        combo:addItem("UI_PersonalKnox_CompanionTwo", true)
        combo:addItem("UI_PersonalKnox_CompanionThree")
        combo:addItem("UI_PersonalKnox_CompanionFour")
    end
    if page and not page:getOption("raidMax") then
        page:addSlider("raidMax", getText("UI_PersonalKnox_RaidMax"), 10, 30, 5, 30,
            getText("UI_PersonalKnox_RaidMaxHint"))
    end
    if page and not page:getOption("raidFrequency") then
        local combo = page:addComboBox("raidFrequency", getText("UI_PersonalKnox_RaidFrequency"),
            getText("UI_PersonalKnox_RaidFrequencyHint"))
        combo:addItem("UI_PersonalKnox_RaidRare")
        combo:addItem("UI_PersonalKnox_RaidNormal", true)
        combo:addItem("UI_PersonalKnox_RaidOften")
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
function Settings.npcFinds() return value("npcFinds", true) end
function Settings.npcStock() return value("npcStock", true) end
function Settings.npcBandageStock() return value("npcBandageStock", true) end
function Settings.autoReload() return Settings.reloadMode() == 2 end
function Settings.npcItemAsks() return value("npcItemAsks", true) end
function Settings.performanceMode() return value("performanceMode", false) end

function Settings.worldgenWrites() return value("worldgenWrites", false) end

function Settings.identityRussianFirst() return value("identityRussianFirst", false) end
function Settings.zombieAttackOwnNpc() return value("zombieAttackOwnNpc", true) end

function Settings.zombiePathMode()
    local option = page and page:getOption("zombiePathMode")
    local choice = option and option:getValue() or 1
    return choice == 2 and "WITH_ROPE" or choice == 3 and "RANDOM" or "NO_ROPE"
end

function Settings.worldgenProtection()
    local option = page and page:getOption("worldgenProtection")
    local value = option and option:getValue() or 0
    return type(value) == "number" and math.max(0, math.min(16, value)) or 0
end

function Settings.worldgenBudget()
    local option = page and page:getOption("worldgenBudget")
    local value = option and option:getValue() or 32
    return type(value) == "number" and math.max(1, math.min(512, value)) or 32
end
function Settings.chatterFrequency()
    local option = page and page:getOption("chatterFrequency")
    if not option then return 3 end
    local choice = option:getValue()
    return type(choice) == "number" and choice or 3
end
function Settings.ammoWeight()
    local option = page and page:getOption("ammoWeight")
    if not option then return 3 end
    local choice = option:getValue()
    return type(choice) == "number" and choice or 3
end
function Settings.ammoWeightFactor()
    local choice = Settings.ammoWeight()
    if choice == 1 then return 0 end
    if choice == 2 then return 0.5 end
    return 1
end
function Settings.inGameTests()
    return value("inGameTests", false)
end

function Settings.matchPlayerClothes()
    return value("matchPlayerClothes", false)
end

function Settings.showStartHint() return value("showStartHint", true) end

function Settings.bodySyncBudgetMs()
    local option = page and page:getOption("bodySyncBudgetMs")
    local value = option and option:getValue() or 8
    return type(value) == "number" and math.max(2, math.min(50, value)) or 8
end

function Settings.bodyMoveBudget()
    local option = page and page:getOption("bodyMoveBudget")
    local value = option and option:getValue() or 8
    return type(value) == "number" and math.max(2, math.min(32, value)) or 8
end

function Settings.activeBodyLimit()
    local option = page and page:getOption("activeBodyLimit")
    local value = option and option:getValue() or 24
    return type(value) == "number" and math.max(4, math.min(64, value)) or 24
end

function Settings.openSquad()
    return value("openSquad", true)
end
function Settings.squadKey()
    local option = page and page:getOption("squadWindowKey")
    return option and option:getValue() or Keyboard.KEY_INSERT
end

function Settings.baseKey()
    local option = page and page:getOption("baseWindowKey")
    return option and option:getValue() or Keyboard.KEY_DELETE
end

function Settings.factionMembers()
    local option = page and page:getOption("factionMembers")
    if not option then return 3 end
    local value = option:getValue()
    return type(value) == "number" and value or 3
end

function Settings.factionSupplies()
    local option = page and page:getOption("factionSupplies")
    if not option then return 2 end
    local value = option:getValue()
    return type(value) == "number" and value or 2
end

function Settings.outpostGarrison()
    local option = page and page:getOption("outpostGarrison")
    if not option then return 1 end
    local value = option:getValue()
    return type(value) == "number" and value or 1
end

function Settings.outpostSupplies()
    local option = page and page:getOption("outpostSupplies")
    if not option then return 1 end
    local value = option:getValue()
    return type(value) == "number" and value or 1
end

function Settings.factionDistance()
    local option = page and page:getOption("factionDistance")
    if not option then return 80 end
    local value = option:getValue()
    return type(value) == "number" and value or 80
end

function Settings.factionCount()
    local option = page and page:getOption("factionCount")
    if not option then return 3 end
    local value = option:getValue()
    return type(value) == "number" and value or 3
end

function Settings.npcWeapon()
    local option = page and page:getOption("npcWeapon")
    if not option then return "melee" end
    local choice = option:getValue()
    if type(choice) == "string" then return choice end
    if type(choice) == "number" then
        local values = {"melee", "firearm", "none"}
        return values[choice] or "melee"
    end
    return "melee"
end

function Settings.factionRanged()
    local option = page and page:getOption("factionRanged")
    return option and option:getValue() or 40
end

function Settings.factionMelee()
    local option = page and page:getOption("factionMelee")
    return option and option:getValue() or 40
end

function Settings.companionCount()
    local option = page and page:getOption("companionCount")
    if not option then return 2 end
    local choice = option:getValue()
    local map = {0, 1, 2, 3, 4}
    return map[choice] or 2
end
function Settings.raidMax()
    local option = page and page:getOption("raidMax")
    if not option then return 30 end
    local value = option:getValue()
    return type(value) == "number" and value or 30
end
function Settings.raidFrequency()
    local option = page and page:getOption("raidFrequency")
    if not option then return 2 end
    local choice = option:getValue()
    return type(choice) == "number" and choice or 2
end
function Settings.raidIntervalMinutes()
    local rate = Settings.raidFrequency()
    if rate == 1 then return 180 end
    if rate == 3 then return 45 end
    return 90
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

-- Base economy sliders (values applied to the Java rules at game start).
local baseRuleSpecs = {
    {"foodCap", "Лимит еды", 10, 1000, 10, 200},
    {"waterCap", "Лимит воды", 10, 1000, 10, 200},
    {"medsCap", "Лимит медицины", 5, 200, 5, 50},
    {"materialsCap", "Лимит материалов", 10, 500, 10, 100},
    {"fuelCap", "Лимит топлива", 5, 300, 5, 60},
    {"ammoCap", "Лимит патронов", 10, 500, 10, 120},
    {"farmYield", "Урожай фермы", 1, 20, 1, 5},
    {"cookMeals", "Блюд из еды", 1, 10, 1, 2},
    {"waterYield", "Вода за цикл", 1, 20, 1, 5},
    {"fishYield", "Улов рыбы", 1, 10, 1, 2},
    {"huntYield", "Добыча охоты", 1, 10, 1, 3},
    {"scavengeFood", "Еда из вылазки", 0, 20, 1, 2},
    {"scavengeMeds", "Медицина из вылазки", 0, 10, 1, 1},
    {"moraleGain", "Прирост морали", 1, 20, 1, 1},
    {"moraleLoss", "Потеря морали", 1, 20, 1, 2},
    {"healAmount", "Лечение из запаса", 10, 100, 5, 50},
    {"buildRate", "Скорость стройки", 1, 20, 1, 4},
    {"generatorHoursPerFuel", "Часов на топливо", 1, 24, 1, 2},
    {"vehicleKmPerFuel", "Км на топливо", 1, 50, 1, 5},
    {"dailyWater", "Вода на жителя", 0, 5, 1, 1},
}
if page then
    for _, spec in ipairs(baseRuleSpecs) do
        local key, label, min, max, step, default = spec[1], spec[2], spec[3], spec[4], spec[5], spec[6]
        if not page:getOption(key) then
            page:addSlider(key, label, min, max, step, default, "Правило экономики базы")
        end
    end
end

local alifeSpecs = {
    {"factionCount", "A-Life: фракций", 1, 24, 1, 12},
    {"patrolCount", "A-Life: патрулей", 0, 24, 1, 4},
    {"eventFrequency", "A-Life: событие, мин", 1, 240, 5, 60},
    {"raidFrequency", "A-Life: рейд, мин", 1, 240, 5, 90},
    {"ambushChance", "A-Life: засада, шанс", 0, 100, 5, 20},
    {"checkpointChance", "A-Life: КПП, шанс", 0, 100, 5, 15},
    {"sniperChance", "A-Life: снайпер, шанс", 0, 100, 5, 5},
    {"punitiveChance", "A-Life: каратели, шанс", 0, 100, 5, 10},
    {"baseRaidChance", "A-Life: рейд на базу, шанс", 0, 100, 5, 15},
    {"nearRadius", "A-Life: ближний радиус", 20, 200, 10, 80},
    {"farRadius", "A-Life: дальний радиус", 100, 1000, 50, 300},
    {"budgetMs", "A-Life: бюджет, мс", 1, 50, 1, 5},
    {"maxSquads", "A-Life: максимум отрядов", 1, 200, 1, 64},
    {"memoryDays", "A-Life: память, дней", 1, 30, 1, 7},
    {"warningChance", "A-Life: предупреждение, шанс", 0, 100, 5, 35},
    {"leaveChance", "A-Life: уход, шанс", 0, 100, 5, 20},
    {"regroupMorale", "A-Life: порог сбора", 0, 100, 5, 30},
    {"woundedPercent", "A-Life: порог раненых", 0, 100, 5, 35},
    {"radioRange", "A-Life: радиус радио", 10, 500, 10, 120},
    {"diagnostics", "A-Life: диагностика", 0, 1, 1, 0},
}
if page then
    for _, spec in ipairs(alifeSpecs) do
        local key, label, min, max, step, default = spec[1], spec[2], spec[3], spec[4], spec[5], spec[6]
        if not page:getOption(key) then
            page:addSlider(key, label, min, max, step, default, "Правило A-Life")
        end
    end
end
function Settings.alifeSetting(key)
    local option = page and page:getOption(key)
    if not option then return nil end
    local value = option:getValue()
    return type(value) == "number" and value or nil
end

local intervalSpecs = {
    {"baseProduceMinutes", "Производство, мин", 1, 30, 1, 1},
    {"baseDailyMinutes", "Сутки базы, мин", 1, 60, 1, 5},
    {"baseCorpseSeconds", "Уборка трупов, сек", 5, 300, 5, 30},
    {"baseFarmMinutes", "Ферма, мин", 1, 30, 1, 1},
    {"baseFacilityMinutes", "Объекты, мин", 1, 30, 1, 1},
    {"baseScavengeMinutes", "Вылазки, мин", 1, 120, 1, 10},
    {"alarmDecaySeconds", "Спад тревоги, сек", 5, 300, 5, 60},
    {"journalMax", "Журнал, записей", 8, 128, 8, 32},
    {"guardPatrolRadius", "Патруль, клеток", 2, 40, 2, 12},
    {"medicThreshold", "Порог медика, процент", 10, 90, 5, 60},
}
if page then
    for _, spec in ipairs(intervalSpecs) do
        local key, label, min, max, step, default = spec[1], spec[2], spec[3], spec[4], spec[5], spec[6]
        if not page:getOption(key) then
            page:addSlider(key, label, min, max, step, default, "Интервал базы")
        end
    end
end

function Settings.interval(key, fallback)
    local option = page and page:getOption(key)
    if not option then return fallback end
    local value = option:getValue()
    return type(value) == "number" and value or fallback
end

function Settings.baseRule(key)
    local option = page and page:getOption(key)
    if not option then return nil end
    local value = option:getValue()
    return type(value) == "number" and value or nil
end

return Settings

if page then
    local previousApply = page.apply
    function page:apply()
        if previousApply then previousApply(self) end
        saveDbSettings()
    end
    loadDbSettings()
end
