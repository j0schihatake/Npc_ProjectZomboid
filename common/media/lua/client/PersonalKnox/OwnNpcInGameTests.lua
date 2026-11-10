local Settings = require "PersonalKnox/Settings"
-- In-game obstacle course for the own-NPC layer. Run from Mod Options or F5.
local Tests = {}
local passed, failed = 0, 0

local function check(name, fn)
    local ok, result = pcall(fn)
    if ok and result ~= false then
        passed = passed + 1
        print("[PersonalKnox][ingame-test] PASS " .. name)
    else
        failed = failed + 1
        print("[PersonalKnox][ingame-test] FAIL " .. name .. " :: " .. tostring(result))
    end
end

function Tests.run(player)
    if not PersonalKnoxAPI or not player then return end
    passed, failed = 0, 0
    local api = PersonalKnoxAPI.getInstance()
    local x, y, z = player:getX(), player:getY(), player:getZ()
    print("[PersonalKnox][ingame-test] start")
    check("class-files", function() return classFilesInstalled == true end)
    check("player-clothed", function()
        local worn = player:getWornItems()
        return worn ~= nil and worn:size() >= 3
    end)
    local id = api:ownCreateCompanion(player)
    local bodies = api:ownBodiesSync()
    check("spawn-returns-id", function() return id ~= nil and id ~= "" end)
    check("body-sync", function() return bodies >= 1 end)
    check("body-registered", function()
        api:ownBodiesSync()
        local inspect = api:ownBodyInspect(id)
        return string.find(inspect, "squareMember=true", 1, true) ~= nil
    end)
    check("summary", function() return string.find(api:ownSummary(id), "id=" .. id, 1, true) ~= nil end)
    check("tools", function() return string.find(api:ownBodyTools(id), "HAMMER", 1, true) ~= nil end)
    check("clothing", function()
        local worn = api:ownBodyWorn(id)
        print("[PersonalKnox][ingame-test] worn=" .. tostring(worn))
        return string.find(worn, "Tshirt", 1, true) ~= nil
                and string.find(worn, "Trousers", 1, true) ~= nil
                and string.find(worn, "Shoes", 1, true) ~= nil
    end)
    check("identity", function()
        local text = api:ownIdentity(id)
        print("[PersonalKnox][ingame-test] identity=" .. tostring(text))
        return text ~= nil and text:find("[", 1, true) ~= nil
            and text:find(":", 1, true) ~= nil
    end)
    check("follow-order", function()
        api:ownCommand(id, "FOLLOW", x, y, z)
        return string.find(api:ownSummary(id), "order=FOLLOW", 1, true) ~= nil
    end)
    check("hold-order", function()
        api:ownCommand(id, "HOLD", x, y, z)
        return string.find(api:ownSummary(id), "order=HOLD", 1, true) ~= nil
    end)
    check("rest-order", function()
        api:ownCommand(id, "REST", x, y, z)
        return string.find(api:ownSummary(id), "order=REST", 1, true) ~= nil
    end)
    check("loot-order", function()
        api:ownCommand(id, "LOOT", x, y, z)
        return string.find(api:ownSummary(id), "order=LOOT", 1, true) ~= nil
    end)
    check("tick", function() return api:ownTick(0.01, 8) >= 0 end)
    check("target-floor", function() return api:ownTargetZSnapshot() ~= nil and api:ownTargetZSnapshot() ~= "" end)
    check("body-inspect", function() return string.find(api:ownBodyInspect(id), "id=" .. id, 1, true) ~= nil end)
    check("damage", function()
        local before = tonumber((api:ownSummary(id):match("health=([%d%.]+)")) or "0")
        api:ownDamage(id, 10)
        local after = tonumber((api:ownSummary(id):match("health=([%d%.]+)")) or "0")
        return after < before
    end)
    check("speak", function() return api:ownSpeakById(id, "Тест связи.") end)
    check("base", function() return string.find(api:ownBaseSet(x, y, z), "base=", 1, true) ~= nil end)
    check("base-guard", function() return api:ownBaseGuardAll() >= 1 end)
    check("body-model", function()
        local inspect = api:ownBodyInspect(id)
        return string.find(inspect, "model=true", 1, true) ~= nil
                and string.find(inspect, "render=true", 1, true) ~= nil
    end)
    check("tools-all", function()
        local tools = api:ownBodyTools(id)
        return string.find(tools, "HAMMER", 1, true) ~= nil
                and string.find(tools, "SCREWDRIVER", 1, true) ~= nil
                and string.find(tools, "SAW", 1, true) ~= nil
    end)
    check("snapshot", function() return api:ownSnapshot() ~= nil and api:ownSnapshot() ~= "" end)
    check("population-snapshot", function() return api:ownPopulationSnapshot() ~= nil and api:ownPopulationSnapshot() ~= "" end)
    check("base-summary", function() return string.find(api:ownBaseSummary(), "base=", 1, true) ~= nil end)
    check("range-clear", function() return api:ownCombatRange(10, false, "clear") == 10 end)
    check("range-scope", function() return api:ownCombatRange(10, true, "clear") == 15 end)
    check("range-rain", function() return api:ownCombatRange(10, false, "rain") == 6.5 end)
    check("range-storm", function() return api:ownCombatRange(10, false, "storm") == 4.5 end)
    check("damage-weapon", function() return api:ownCombatDamage(1, 0, 10, 10, false) == 10 end)
    check("damage-skill", function() return api:ownCombatDamage(1, 3, 10, 10, false) == 16 end)
    check("damage-broken", function() return api:ownCombatDamage(1, 0, 0, 10, false) == 5 end)
    check("damage-ranged", function() return api:ownCombatDamage(5, 5, 10, 10, true) == 30 end)
    check("factions", function() return api:alifeOutposts() >= 1 end)
    check("combat", function()
        local spawned = api:ownDebugZombie(id)
        if string.find(spawned, "health=", 1, true) == nil then return false end
        local before = api:ownDebugZombieHealth()
        api:ownDebugAttack(id)
        local damaged = api:ownDebugZombieHealth() < before
        api:ownDebugZombieClear()
        return damaged
    end)
    check("ranged-combat", function()
        local spawned = api:ownDebugZombie(id)
        if string.find(spawned, "health=", 1, true) == nil then return false end
        local equip = api:ownDebugEquip(id, "Base.Pistol", 5)
        if string.find(equip, "equipped=", 1, true) == nil then return false end
        local before = api:ownDebugZombieHealth()
        api:ownDebugAttack(id)
        local damaged = api:ownDebugZombieHealth() < before
        local ammo = api:ownDebugAmmo(id)
        api:ownDebugZombieClear()
        return damaged and ammo == 4
    end)
    check("mobility-plan-craft", function()
        local plan = api:mobilityPlan(20, true, false, 0.4, true, true,
            "Base.Plank=2;Base.RippedSheets=2;Base.Nails=3", true, true, true, 0)
        return string.find(plan, "action=CRAFT", 1, true) ~= nil
            and string.find(plan, "PersonalKnox.Crutch", 1, true) ~= nil
            and string.find(plan, "projected=45", 1, true) ~= nil
    end)
    check("mobility-plan-ready", function()
        local plan = api:mobilityPlan(20, true, false, 0.4, true, true,
            "PersonalKnox.Crutch=1;Base.Plank=2", false, true, true, 0)
        return string.find(plan, "action=USE_READY", 1, true) ~= nil
            and string.find(plan, "PersonalKnox.Crutch", 1, true) ~= nil
    end)
    check("upkeep-repair", function() return api:upkeepNeedsRepair(1, 10) end)
    check("upkeep-ammo", function() return api:upkeepAmmo(0, 30) > 0 end)
    check("durability-setting", function() return Settings.infiniteDurability() == true end)
    check("command-all", function() return api:ownCommandAll("REST", x, y, z) >= 1 end)
    check("command-all-follow", function() return api:ownCommandAll("FOLLOW", x, y, z) >= 1 end)
    check("retreat-away", function()
        local p = api:ownRetreatPoint(1, 0, 0, 0, 6)
        return string.find(p, "x=3.00", 1, true) ~= nil
    end)
    check("retreat-hold", function()
        local p = api:ownRetreatPoint(8, 0, 0, 0, 6)
        return string.find(p, "x=8.00", 1, true) ~= nil
    end)
    check("squad-panel", function()
        local ok, panel = pcall(require, "PersonalKnox/OwnNpcPanel")
        if not ok or not panel then return false end
        panel.toggle(player)
        local open = panel.isOpen()
        panel.closeOpen()
        return open
    end)
    check("dismantle", function()
        local obstacle = api:ownDebugObstacle(id)
        if obstacle == nil or obstacle == "" then return false end
        local started = api:ownDismantleStart(id)
        if started == nil or started == "" then return false end
        api:ownDismantleTick(id, 30)
        return true
    end)
    check("give-items", function()
        api:ownDebugEquip(id, "Base.Pistol", 0)
        return api:ownGiveItems(id, player) >= 1
    end)
    check("starting-weapon", function()
        local mode = api:ownStartingWeapon()
        return mode == "melee" or mode == "firearm" or mode == "none"
    end)
    check("auto-equip", function()
        api:ownDebugEquip(id, "Base.BaseballBat", 0)
        local equipped = api:ownAutoEquip(id)
        return equipped ~= nil and equipped ~= "" and equipped ~= "no-weapon"
    end)
    check("npc-drink", function()
        api:ownDebugItem(id, "Base.TestWaterMug")
        api:ownDebugSetNeeds(id, 0.9, 0.9)
        local consumed = api:ownDrink(id)
        local needs = api:ownNeeds(id)
        return consumed ~= nil and string.find(consumed, "Base.TestWaterMug", 1, true) ~= nil
            and string.find(needs, "thirst=0.400", 1, true) ~= nil
    end)
    check("npc-eat", function()
        api:ownDebugItem(id, "Base.Bread")
        api:ownDebugSetNeeds(id, 0.9, 0.9)
        local consumed = api:ownEat(id)
        local needs = api:ownNeeds(id)
        return consumed ~= nil and string.find(consumed, "Base.Bread", 1, true) ~= nil
            and string.find(needs, "hunger=0.400", 1, true) ~= nil
    end)
    check("auto-eat", function()
        api:ownDebugSupplies(id, 5)
        api:ownDebugItem(id, "Base.Bread")
        api:ownDebugSetNeeds(id, 0.9, 0.9)
        api:ownBodiesSync()
        return api:ownSupplies(id) == 4
    end)
    check("supply-use", function()
        api:ownDebugSupplies(id, 5)
        api:ownDebugItem(id, "Base.Bread")
        api:ownDebugSetNeeds(id, 0.9, 0.9)
        api:ownEat(id)
        return api:ownSupplies(id) == 4
    end)
    check("loot-container", function()
        api:ownDebugSupplies(id, 0)
        if api:ownDebugPlaceLoot(id) ~= "placed" then return false end
        local result = api:ownDebugLoot(id)
        return result ~= nil and result ~= "no-container" and api:ownSupplies(id) >= 1
    end)
    check("bandage", function()
        api:ownDamage(id, 20)
        local before = api:ownHealth(id)
        api:ownDebugItem(id, "Base.Bandage")
        local result = api:ownBandage(id)
        local after = api:ownHealth(id)
        return result ~= nil and string.find(result, "bandaged", 1, true) ~= nil and after > before
    end)
    check("npc-inventory", function()
        api:ownDebugItem(id, "Base.Bread")
        local inventory = api:ownInventory(id)
        return inventory ~= nil and string.find(inventory, "Base.Bread", 1, true) ~= nil
    end)
    check("give-weapon", function()
        api:ownDebugPlayerItem(player, "Base.Pistol")
        local gave = api:ownGiveWeapon(id, player)
        local hand = api:ownPrimaryItem(id)
        return gave ~= nil and string.find(gave, "Base.Pistol", 1, true) ~= nil
            and string.find(hand, "Base.Pistol", 1, true) ~= nil
    end)
    check("give-item", function()
        api:ownDebugPlayerItem(player, "Base.Bread")
        local gave = api:ownGiveItem(id, player, "Base.Bread")
        local inventory = api:ownInventory(id)
        return gave ~= nil and string.find(gave, "Base.Bread", 1, true) ~= nil
            and string.find(inventory, "Base.Bread", 1, true) ~= nil
    end)
    check("weapon-slot", function()
        local holstered = api:ownHolster(id)
        local empty = api:ownPrimaryItem(id)
        local drawn = api:ownDraw(id)
        local hand = api:ownPrimaryItem(id)
        return string.find(holstered, "holstered ", 1, true) ~= nil
            and empty == "" and string.find(drawn, "drawn ", 1, true) ~= nil
            and hand ~= ""
    end)
    check("auto-weapon", function()
        api:ownHolster(id)
        local drawn = api:ownAutoWeapon(id, true)
        local armed = api:ownPrimaryItem(id)
        local holstered = api:ownAutoWeapon(id, false)
        local empty = api:ownPrimaryItem(id)
        return string.find(drawn, "drawn ", 1, true) ~= nil and armed ~= ""
            and string.find(holstered, "holstered ", 1, true) ~= nil and empty == ""
    end)
    check("group-draw", function()
        api:ownHolsterAll()
        local drawn = api:ownDrawAll()
        local armed = api:ownArmedCount()
        return drawn ~= nil and drawn >= 2 and armed >= 2
    end)
    check("alert-phrase", function()
        local first = api:ownAlertPhrase(0)
        local second = api:ownAlertPhrase(1)
        return first ~= nil and first ~= "" and second ~= nil and second ~= "" and first ~= second
    end)
    check("equipment-snapshot", function()
        api:ownHolster(id)
        local snapshot = api:ownEquipmentSnapshot()
        api:ownDraw(id)
        return snapshot ~= nil and string.find(snapshot, "=H:", 1, true) ~= nil
    end)
    check("equipment-restore", function()
        api:ownHolster(id)
        local snapshot = api:ownEquipmentSnapshot()
        api:ownDraw(id)
        api:ownRestoreEquipment(snapshot)
        return api:ownPrimaryItem(id) == ""
    end)
    check("equipment-persist", function()
        api:ownDebugEquip(id, "Base.Pistol", 0)
        api:ownBodiesSync()
        return string.find(api:ownLastWeapon(id), "Base.Pistol", 1, true) ~= nil
    end)
    check("base-staffing", function()
        local plan = api:ownBaseStaffing(8, "GUARD,FARMER,COOK,WATER")
        return string.find(plan, "GUARD=2", 1, true) ~= nil
            and string.find(plan, "FARMER=2", 1, true) ~= nil
    end)
    check("profession", function()
        local assigned = api:ownSetProfession(id, "FARMER")
        return assigned == "FARMER" and api:ownProfession(id) == "FARMER"
            and string.find(api:ownSummary(id), "profession=FARMER", 1, true) ~= nil
    end)
    check("corpse-cleanup", function()
        local spawned = api:ownDebugZombie(id)
        if string.find(spawned, "health=", 1, true) == nil then return false end
        api:ownDebugKillZombie()
        api:ownBodiesSync()
        local cleaned = api:ownCleanCorpses(20)
        api:ownDebugZombieClear()
        return cleaned ~= nil and cleaned >= 0
    end)
    check("profession-action", function()
        api:ownSetProfession(id, "CORPSE")
        return api:ownHasProfession("CORPSE") == true
    end)
    check("base-produce", function()
        api:ownSetProfession(id, "WATER")
        local stock = api:ownBaseProduce("WATER")
        return stock ~= nil and string.find(stock, "water=5", 1, true) ~= nil
    end)
    check("base-feed", function()
        api:ownBaseProduce("WATER")
        api:ownSetProfession(id, "WATER")
        api:ownDebugSetNeeds(id, 0.2, 0.9)
        api:ownBodiesSync()
        local needs = api:ownNeeds(id)
        return string.find(needs, "thirst=0.400", 1, true) ~= nil
    end)
    check("base-persist", function()
        api:ownBaseProduce("WATER")
        api:ownSetProfession(id, "WATER")
        local snapshot = api:ownBaseSnapshot()
        api:ownRestoreBase(snapshot)
        return string.find(snapshot, "water=", 1, true) ~= nil
            and string.find(snapshot, "prof=" .. id .. ":WATER", 1, true) ~= nil
    end)
    check("give-clothing", function()
        api:ownDebugPlayerItem(player, "Base.Tshirt_ArmyGreen")
        local wore = api:ownGiveClothing(id, player)
        return wore ~= nil and string.find(wore, "wore ", 1, true) ~= nil
    end)
    check("farm-cycle", function()
        api:ownSetProfession(id, "FARMER")
        local last = ""
        for i = 1, 4 do last = api:ownFarmTick(true) end
        return string.find(last, "harvest=5", 1, true) ~= nil
    end)
    check("base-cook", function()
        api:ownBaseProduce("FARMER")
        local cooked = api:ownBaseCook()
        return cooked ~= nil and string.find(cooked, "meals=", 1, true) ~= nil
    end)
    check("blueprint", function()
        api:ownSetProfession(id, "BUILD")
        local started = api:ownBlueprintStart(4, 4)
        local state = started
        for i = 1, 8 do state = api:ownBlueprintTick(4) end
        return string.find(state, "complete=true", 1, true) ~= nil
    end)
    check("base-daily", function()
        api:ownBaseProduce("WATER")
        api:ownBaseProduce("FARMER")
        local before = api:ownBaseStock()
        local after = api:ownBaseDaily(1)
        return before ~= after and string.find(after, "people=1", 1, true) ~= nil
    end)
    check("base-work-persist", function()
        api:ownFarmTick(true)
        api:ownBlueprintStart(4, 4)
        local snapshot = api:ownBaseSnapshot()
        api:ownRestoreBase(snapshot)
        return string.find(snapshot, "farm=", 1, true) ~= nil
            and string.find(snapshot, "bp=", 1, true) ~= nil
    end)
    check("base-economy", function()
        api:ownBaseProduce("WATER")
        api:ownBaseProduce("FARMER")
        local heal = api:ownBaseHeal()
        local took = api:ownBaseTake(1, 1, 0)
        local scav = api:ownBaseScavenge(1)
        return heal ~= nil and took ~= nil and scav ~= nil
    end)
    check("base-guard", function()
        api:ownSetProfession(id, "GUARD")
        local defense = api:ownBaseDefense()
        return string.find(defense, "guards=1", 1, true) ~= nil
    end)
    check("base-medic", function()
        api:ownBaseProduce("BUILD")
        local healed = api:ownHealFromBase(id)
        return healed ~= nil and (string.find(healed, "healed", 1, true) ~= nil
            or string.find(healed, "no-meds", 1, true) ~= nil)
    end)
    check("base-logistics", function()
        api:ownBaseProduce("BUILD")
        local ammo = api:ownBaseCraftAmmo()
        local fish = api:ownBaseFish()
        local laundry = api:ownBaseLaundry()
        local repair = api:ownBaseRepair()
        return ammo ~= nil and fish ~= nil and laundry ~= nil and repair ~= nil
    end)
    check("base-ops", function()
        local shifts = api:ownGuardShifts(5, 2)
        local ops = api:ownBaseOps()
        return shifts ~= nil and shifts[1] ~= nil and ops ~= nil
            and api:ownStorageCap("FOOD") == 200
            and string.find(api:ownEquipmentTemplate("GUARD"), "pistol", 1, true) ~= nil
    end)
    check("base-rules", function()
        api:ownBaseRule("foodCap", 123)
        return string.find(api:ownBaseRules(), "foodCap=123", 1, true) ~= nil
            and api:ownStorageCap("FOOD") == 123
    end)
    check("base-actions", function()
        local sleep = api:ownBaseSleep()
        local radio = api:ownBaseRadio()
        local guard = api:ownBaseGuardDuty()
        local recipe = api:ownBaseCookRecipe()
        return sleep ~= nil and radio ~= nil and guard ~= nil and recipe ~= nil
    end)
    check("facilities", function()
        for i = 1, 4 do api:ownBaseProduce("BUILD") end
        local built = api:ownBuildFacility("FARM")
        local produced = api:ownFacilityTick()
        return built ~= nil and string.find(built, "built FARM", 1, true) ~= nil
            and produced ~= nil and string.find(produced, "food=", 1, true) ~= nil
    end)
    check("base-control", function()
        api:ownBaseAlarm(3)
        api:ownAlarmTick()
        api:ownAddWounded(3)
        api:ownRotateWounded()
        local status = api:ownBaseStatus()
        return status ~= nil and string.find(status, "alarm=2", 1, true) ~= nil
            and string.find(status, "wounded=0", 1, true) ~= nil
    end)
    check("base-rule-count", function()
        return api:ownBaseRuleCount() == 30 and api:ownFacilityCost("FARM") > 0
    end)
    check("alife-core", function()
        local demo = api:ownAlifeCoreDemo()
        return demo ~= nil and string.find(demo, "winner=", 1, true) ~= nil
            and string.find(demo, "roster=32", 1, true) ~= nil
    end)
    check("alife-operation", function()
        local result = api:ownAlifeOperation(7)
        return result ~= nil and string.find(result, "winner=", 1, true) ~= nil
            and string.find(result, "encounters=1", 1, true) ~= nil
    end)
    check("alife-patrol", function()
        local result = api:ownAlifePatrol()
        return result ~= nil and string.find(result, "route=", 1, true) ~= nil
            and string.find(result, "situation=", 1, true) ~= nil
    end)
    check("alife-runtime", function()
        api:alifeSilence("outpost1", 1, 2)
        local result = api:alifeOperation(8, 6, 10, 7, 2, "marauders", "outpost1")
        return result ~= nil and string.find(result, "winner=", 1, true) ~= nil
            and api:alifeSilent("outpost1", 2) == true
    end)
    check("alife-policy", function()
        local result = api:ownAlifePolicy()
        return result ~= nil and string.find(result, "encounter=", 1, true) ~= nil
            and string.find(result, "editor=true", 1, true) ~= nil
    end)
    check("alife-campaign", function()
        local report = api:ownAlifeCampaign(3, 5)
        return report ~= nil and string.find(report, "day=3", 1, true) ~= nil
            and string.find(report, "situation=", 1, true) ~= nil
    end)
    check("alife-world", function()
        api:alifeWorldSilence("outpost1", 1, 2)
        local result = api:alifeWorldOperation(8, 6, 10, 7, 2, "marauders", "outpost1")
        return result ~= nil and string.find(result, "winner=", 1, true) ~= nil
            and api:alifeWorldSilent("outpost1", 2) == true
            and api:alifeEncounterCount() >= 1
    end)
    check("alife-bodies", function()
        local count = api:ownAlifeMaterialize(player, 4)
        return count ~= nil and count >= 0
    end)
    check("alife-advanced", function()
        local result = api:ownAlifeAdvanced()
        return result ~= nil and string.find(result, "identity=", 1, true) ~= nil
            and string.find(result, "bodies=", 1, true) ~= nil
    end)
    check("alife-aftermath", function()
        api:alifeWorldOperation(8, 6, 10, 7, 2, "marauders", "outpost1")
        local aftermath = api:alifeAftermath()
        local radio = api:alifeRadioCall("patrol", 2, 10)
        return aftermath ~= nil and string.find(aftermath, "bodies=", 1, true) ~= nil
            and radio ~= nil and string.find(radio, "answer", 1, true) ~= nil
    end)
    check("alife-settings", function()
        local set = api:alifeSetting("eventFrequency", 30)
        return set ~= nil and string.find(set, "eventFrequency=30", 1, true) ~= nil
    end)
    check("alife-director", function()
        local choice = api:alifeDirector(4, 10)
        return choice ~= nil and string.find(choice, "situation=", 1, true) ~= nil
    end)
    check("alife-interaction", function()
        local good = api:ownAlifeInteraction(60)
        local bad = api:ownAlifeInteraction(-60)
        return good ~= nil and bad ~= nil
            and string.find(good, "hostile=false", 1, true) ~= nil
            and string.find(bad, "hostile=true", 1, true) ~= nil
    end)
    check("base-defense-plan", function()
        local plan = api:ownBaseDefensePlan(4, 6, 2)
        return string.find(plan, "windows=2", 1, true) ~= nil
            and string.find(plan, "reserve=2", 1, true) ~= nil
            and string.find(plan, "hidden=6", 1, true) ~= nil
    end)
    check("faction-chances", function()
        api:ownSetFactionChances(30, 30)
        local ranged = api:ownFactionKitRoll("Military", 10)
        local melee = api:ownFactionKitRoll("Military", 50)
        local none = api:ownFactionKitRoll("Military", 90)
        return string.find(ranged, "Base.Pistol", 1, true) ~= nil
            and string.find(melee, "Base.BaseballBat", 1, true) ~= nil
            and string.find(none, "Base.Hammer", 1, true) ~= nil
    end)
    check("faction-kit", function()
        local police = api:ownFactionKit("Military")
        local raiders = api:ownFactionKit("Raiders")
        return string.find(police, "Base.Pistol", 1, true) ~= nil
            and string.find(raiders, "Base.Hat_Bandana", 1, true) ~= nil
    end)
    check("faction-groups", function()
        return api:ownGroupCount() >= 1
    end)
    check("snapshot-restore", function()
        local before = api:ownSnapshot()
        if before == nil or before == "" then return false end
        local restored = api:ownRestore(before)
        return restored ~= nil and restored >= 1
    end)
    check("formation", function()
        if api:ownCount() < 2 then return true end
        api:ownCommandAll("FOLLOW", x, y, z)
        local a = api:ownPosition(api:ownId(0))
        local b = api:ownPosition(api:ownId(1))
        return a ~= nil and b ~= nil and a ~= b
    end)
    check("own-medic-splint", function()
        local patientId = api:ownId(0)
        local medicId = api:ownId(1)
        local patientBody = api:ownBodyObject(patientId)
        local medicBody = api:ownBodyObject(medicId)
        if not patientBody or not medicBody then
            print("[PersonalKnox][ingame-test] own-medic missing body patient="
                .. tostring(patientBody) .. " medic=" .. tostring(medicBody))
            return false
        end
        medicBody:setX(patientBody:getX())
        medicBody:setY(patientBody:getY())
        medicBody:setZ(patientBody:getZ())
        medicBody:getInventory():AddItem("Base.Splint")
        local part = patientBody:getBodyDamage():getBodyPart(BodyPartType.LowerLeg_L)
        part:setFractureTime(30)
        part:setSplint(false, 0)
        local OwnNpcMedic = require "PersonalKnox/OwnNpcMedic"
        local result = OwnNpcMedic.tick(patientId, medicId, true)
        print("[PersonalKnox][ingame-test] own-medic result=" .. tostring(result)
            .. " fracture=" .. tostring(part:getFractureTime()))
        return result == "splint"
    end)
    check("own-medic-field-splint", function()
        local patientId = api:ownId(0)
        local medicId = api:ownId(1)
        local patientBody = api:ownBodyObject(patientId)
        local medicBody = api:ownBodyObject(medicId)
        if not patientBody or not medicBody then return false end
        medicBody:setX(patientBody:getX())
        medicBody:setY(patientBody:getY())
        medicBody:setZ(patientBody:getZ())
        medicBody:getInventory():AddItem("Base.RippedSheets")
        medicBody:getInventory():AddItem("Base.Plank")
        local part = patientBody:getBodyDamage():getBodyPart(BodyPartType.LowerLeg_L)
        part:setFractureTime(30)
        part:setSplint(false, 0)
        local OwnNpcMedic = require "PersonalKnox/OwnNpcMedic"
        local result = OwnNpcMedic.tick(patientId, medicId, true)
        print("[PersonalKnox][ingame-test] own-medic-field result=" .. tostring(result))
        return result == "splint"
    end)
    check("npc-gender-visual", function()
        local female, mismatch = 0, 0
        for i = 0, api:ownCount() - 1 do
            local body = api:ownBodyObject(api:ownId(i))
            if body then
                local desc = body:getDescriptor()
                local visual = body:getHumanVisual()
                if desc and visual and desc:isFemale() then
                    female = female + 1
                    if not visual:isFemale() then mismatch = mismatch + 1 end
                end
            end
        end
        print("[PersonalKnox][ingame-test] npc-gender female=" .. female .. " mismatch=" .. mismatch)
        return female > 0 and mismatch == 0
    end)
    check("worldgen-write", function()
        local worldgen = PersonalWorldGenAPI.getInstance()
        local cell = getCell()
        local px, py, pz = player:getX(), player:getY(), player:getZ()
        local target = nil
        for _, d in ipairs({{30,0},{-30,0},{0,30},{0,-30},{20,20},{-20,20},{20,-20},{-20,-20}}) do
            local sq = cell and cell:getGridSquare(px + d[1], py + d[2], pz)
            if sq and sq:isFree(false) then
                target = {px + d[1], py + d[2]}
                break
            end
        end
        if not target then
            print("[PersonalKnox][ingame-test] worldgen-write no-free-square")
            return false
        end
        local settingsBefore = worldgen:worldgenSettings()
        worldgen:setWriteBudget(8)
        worldgen:setWritesEnabled(true)
        local ok, result = pcall(function()
            player:setX(target[1])
            player:setY(target[2])
            player:setZ(pz)
            local text = worldgen:generateTestSite(player)
            player:setX(px)
            player:setY(py)
            player:setZ(pz)
            return text
        end)
        worldgen:setWritesEnabled(false)
        worldgen:setWriteBudget(32)
        if not ok then
            pcall(function() player:setX(px); player:setY(py); player:setZ(pz) end)
            print("[PersonalKnox][ingame-test] worldgen-write error=" .. tostring(result))
            return false
        end
        local used = tonumber(tostring(result):match("used=(%d+)"))
        print("[PersonalKnox][ingame-test] worldgen-write " .. tostring(result)
            .. " default=" .. tostring(settingsBefore) .. " used=" .. tostring(used))
        return tostring(settingsBefore):find("writes=off", 1, true) ~= nil
            and tostring(result):find("worldWrites=", 1, true) ~= nil
            and tostring(result):find("worldWrites=0", 1, true) == nil
            and tostring(result):find("budget=8", 1, true) ~= nil
            and used ~= nil and used <= 8
    end)
    check("worldgen-registry", function()
        local api = PersonalWorldGenAPI.getInstance()
        api:restoreGenerated("wg-a,wg-b,wg-a")
        local csv = api:generatedRegions()
        return csv:find("wg-a", 1, true) ~= nil and csv:find("wg-b", 1, true) ~= nil
    end)
    check("worldgen-plan", function()
        local plan = PersonalWorldGenAPI.getInstance():planNearPlayer(player, 42)
        print("[PersonalKnox][ingame-test] worldgen-plan " .. tostring(plan))
        return plan ~= nil and plan:find("worldWrites=0", 1, true) ~= nil
            and (plan:find("campOps=", 1, true) ~= nil or plan:find("protected=true", 1, true) ~= nil)
    end)
    check("remove", function()
        local before = api:ownActive()
        api:ownRemove(id)
        return api:ownActive() == before - 1
    end)
    check("alife-squads", function() return api:alifeSquads() >= 1 end)
    if player then player:getModData().PersonalKnoxTestsDone = getTimestampMs() end
    print("[PersonalKnox][ingame-test] summary passed=" .. tostring(passed) .. " failed=" .. tostring(failed))
    return failed == 0
end

return Tests
