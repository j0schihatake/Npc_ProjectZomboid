package personal.knox;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import me.zed_0xff.zombie_buddy.Exposer;
import personal.knox.npc.NpcNeeds;
import personal.knox.ItemNeedPolicy;

@Exposer.LuaClass
public final class PersonalKnoxAPI {
    private record Dialogue(int id, String topic, String condition, String text) {}
    private static final PersonalKnoxAPI INSTANCE = new PersonalKnoxAPI();
    private final List<Dialogue> dialogues;
    private final ReactionDirector reactions = new ReactionDirector();
    private final PlayerCare care = new PlayerCare();
    public static PersonalKnoxAPI getInstance() { return INSTANCE; }
    private final personal.knox.npc.own.OwnNpcPopulation ownPopulation = new personal.knox.npc.own.OwnNpcPopulation();
    private final personal.knox.npc.own.OwnNpcLayer ownLayer = new personal.knox.npc.own.OwnNpcLayer();
    private final personal.knox.npc.own.OwnNpcBodyAdapter ownBodies = new personal.knox.npc.own.OwnNpcBodyAdapter();
    private final personal.knox.npc.ZombiePathPolicy zombiePath = new personal.knox.npc.ZombiePathPolicy();
    private final personal.knox.db.ModDatabase database = personal.knox.db.ModDatabase.getInstance();

    public void ownGroup(String id, String faction, int members, double x, double y) {
        int effective = members;
        if ("player-squad".equals(id) && effective <= 0) effective = ownLayer.count();
        var group = new personal.knox.npc.own.OwnNpcPopulation.Group(id, faction,
                personal.knox.npc.own.OwnNpcPopulation.Group.Kind.PLAYER_SQUAD,
                effective, x, y, x, y);
        if (ownPopulation.group(id) == null) ownPopulation.add(group);
        else ownPopulation.update(group);
    }

    public int ownSync(double px, double py, double radius, int perGroup) {
        return personal.knox.npc.own.OwnNpcSpawner.sync(ownPopulation, ownLayer, px, py, radius, perGroup);
    }

    public String knoxStatus() { return KnoxBridge.status(); }
    public int knoxGroupMembers(Object player) { return KnoxBridge.groupMembers(player); }
    public double knoxSafehouseX(Object player) {
        var p = KnoxBridge.safehousePosition(player);
        return p == null ? 0 : p[0];
    }

    public double knoxSafehouseY(Object player) {
        var p = KnoxBridge.safehousePosition(player);
        return p == null ? 0 : p[1];
    }


    public String knoxMemberSnapshot(Object player) { return KnoxBridge.groupMemberSnapshot(player); }
    public int knoxMemberCount(Object player) {
        return KnoxRoster.parse(KnoxBridge.groupMemberSnapshot(player)).size();
    }

    public String knoxMemberIds(Object player) {
        var members = KnoxRoster.parse(KnoxBridge.groupMemberSnapshot(player));
        var text = new java.lang.StringBuilder();
        for (var member : members) text.append(member.id()).append(';');
        return text.toString();
    }







    public String ownTextForChat(String text) { return TextCodec.forChat(text); }

    public boolean ownSpeakById(String id, String text) {
        Object body = ownBodies.body(id);
        if (body == null) return false;
        ownSpeak(body, text);
        return true;
    }

    public void ownSpeak(Object speaker, String text) {
        if (speaker == null) return;
        String value = TextCodec.forChat(text);
        try {
            speaker.getClass().getMethod("addLineChatElement", String.class, float.class, float.class, float.class)
                    .invoke(speaker, value, 1f, 1f, 1f);
        } catch (Throwable ignored) {
            try { speaker.getClass().getMethod("Say", String.class).invoke(speaker, value); }
            catch (Throwable ignored2) { }
        }
    }

    public String dbSetting(String key, String fallback) { return database.getSetting(key, fallback); }
    public void dbSetSetting(String key, String value) { database.setSetting(key, value); }
    public String dbSettings() { return database.allSettings(); }
    public String dbData(String kind, String id, String fallback) { return database.getData(kind, id, fallback); }
    public void dbPutData(String kind, String id, String value) { database.putData(kind, id, value); }

    public void zombiePathMode(String mode) { zombiePath.setMode(mode); }
    public String zombieRetarget(Object zombie) {
        if (zombie == null) return "no-zombie";
        try {
            double zx = ((Number) zombie.getClass().getMethod("getX").invoke(zombie)).doubleValue();
            double zy = ((Number) zombie.getClass().getMethod("getY").invoke(zombie)).doubleValue();
            double zz = ((Number) zombie.getClass().getMethod("getZ").invoke(zombie)).doubleValue();
            Object current = zombie.getClass().getMethod("getTarget").invoke(zombie);
            Object player = Class.forName("zombie.characters.IsoPlayer").getMethod("getInstance").invoke(null);
            Object body = ownBodies.nearestBody(zx, zy, zz, 12);
            if (body == null) return "";
            if (player != null) {
                double px = ((Number) player.getClass().getMethod("getX").invoke(player)).doubleValue();
                double py = ((Number) player.getClass().getMethod("getY").invoke(player)).doubleValue();
                double playerDistance = Math.hypot(px - zx, py - zy);
                double bodyDistance = Math.hypot(
                        ((Number) body.getClass().getMethod("getX").invoke(body)).doubleValue() - zx,
                        ((Number) body.getClass().getMethod("getY").invoke(body)).doubleValue() - zy);
                if (playerDistance + 1.0 < bodyDistance) return "";
            }
            if (current == body) return "already";
            zombie.getClass().getMethod("setTarget", Class.forName("zombie.iso.IsoMovingObject")).invoke(zombie, body);
            zombie.getClass().getMethod("pathToCharacter", Class.forName("zombie.characters.IsoGameCharacter")).invoke(zombie, body);
            return "target=" + ownBodies.bodyId(body);
        } catch (Throwable error) {
            return "retarget-error=" + error.getClass().getSimpleName();
        }
    }

    public String zombiePathStatus() { return zombiePath.status(); }
    public boolean zombiePathAllows(String id, boolean crawling) { return zombiePath.ropeAllowed(id, crawling); }

    public String ownIdentity(String id) { return ownBodies.identitySummary(id); }
    public String ownBodyId(Object body) { return ownBodies.bodyId(body); }
    public int ownPersistIdentities() { return ownBodies.persistIdentities(); }
    public int ownLoadIdentities() { return ownBodies.loadIdentities(); }

    public String ownIdentitySnapshot() { return ownBodies.identitySnapshot(); }
    public int ownRestoreIdentities(String snapshot) { return ownBodies.restoreIdentities(snapshot); }

    public String ownSummary(String id) {
        var agent = ownLayer.agents().stream().filter(a -> a.id().equals(id)).findFirst().orElse(null);
        if (agent == null) return "";
        var tools = new StringBuilder();
        for (var tool : ownBodies.tools(id)) tools.append(tool.name()).append(';');
        var identity = ownBodies.identitySummary(id);
        return (identity.isEmpty() ? "" : identity + " ") + "id=" + id + " order=" + ownLayer.orderOf(id)
                + " task=" + agent.state().task()
                + " health=" + String.format(java.util.Locale.ROOT, "%.0f", agent.state().health())
                + " supplies=" + agent.supplies()
                + " profession=" + ownLayer.profession(id)
                + " pos=" + String.format(java.util.Locale.ROOT, "%.1f,%.1f,%.1f", agent.x(), agent.y(), agent.z())
                + " tools=" + tools;
    }

    public int ownActive() { return ownLayer.count(); }
    public int ownGroupCount() { return ownPopulation.groups().size(); }
    public String ownBaseDefensePlan(int armed, int unarmed, int windows) {
        var plan = personal.knox.npc.own.OwnBaseDefense.plan(armed, unarmed, windows);
        return "windows=" + plan.windows() + " reserve=" + plan.reserve()
                + " hidden=" + plan.hidden() + " withoutCover=" + plan.unarmedWithoutCover();
    }

    public String ownFactionKit(String faction) {
        var kit = personal.knox.npc.own.OwnNpcFactionKit.kit(faction,
                factionRanged, factionMelee, 0);
        return "weapon=" + kit.weapon() + " accessory=" + kit.accessory();
    }

    private volatile int factionRanged = 40;
    private volatile int factionMelee = 40;

    public void ownSetFactionChances(int ranged, int melee) {
        factionRanged = Math.max(0, Math.min(100, ranged));
        factionMelee = Math.max(0, Math.min(100 - factionRanged, melee));
        ownBodies.setFactionChances(factionRanged, factionMelee);
    }

    public String ownFactionChances() { return factionRanged + "/" + factionMelee; }

    public String ownFactionKitRoll(String faction, int roll) {
        return ownBodies.factionKitRoll(faction, roll);
    }
    public void ownGroupMission(String groupId, double x, double y, double z) {
        ownLayer.groupMission(groupId, x, y, z);
    }

    public String ownDamage(String id, double amount) {
        return ownLayer.damage(id, amount).type().name();
    }

    public String ownCommand(String id, String mode, double x, double y, double z) {
        String order = mode == null ? "" : mode.toUpperCase(java.util.Locale.ROOT);
        ownLayer.order(id, personal.knox.npc.own.OwnNpcOrder.valueOf(order), x, y);
        if ("FOLLOW".equals(order)) ownLayer.followTarget(x, y, z);
        return id;
    }

    public int ownCommandAll(String mode, double x, double y, double z) {
        String order = mode == null ? "" : mode.toUpperCase(java.util.Locale.ROOT);
        var value = personal.knox.npc.own.OwnNpcOrder.valueOf(order);
        int count = 0;
        for (var agent : ownLayer.agents()) {
            ownLayer.order(agent.id(), value, x, y);
            count++;
        }
        if ("FOLLOW".equals(order)) ownLayer.followTarget(x, y, z);
        return count;
    }

    public String ownOrderMode(String id, String order, double x, double y) {
        return ownLayer.order(id, personal.knox.npc.own.OwnNpcOrder.valueOf(order.toUpperCase(java.util.Locale.ROOT)), x, y).agentId();
    }

    public void ownFollowTarget(double x, double y) { ownLayer.followTarget(x, y); }

    public void ownFollowTarget3(double x, double y, double z) { ownLayer.followTarget(x, y, z); }

    public double ownTargetZ(String id) { return ownLayer.targetZ(id); }
    public String ownDismantleNearby(String id) { return ownBodies.dismantleNearby(id); }
    public String ownDismantlePlan(String id, String obstacle) { return ownBodies.plan(id, obstacle); }
    public String ownNearbyObstacle(String id) { return ownBodies.nearbyObstacle(id); }




    public String ownDismantleStart(String id) { return ownBodies.startDismantle(id); }
    public String ownDebugObstacle(String id) { return ownBodies.debugFindObstacle(id); }
    public String ownPosition(String id) { return ownBodies.position(id); }
    public String ownFactionPosition(int index) {
        int seen = 0;
        for (var agent : ownLayer.agents()) {
            if (!agent.id().startsWith("own-faction")) continue;
            if (seen++ == index) return ownBodies.position(agent.id());
        }
        return "";
    }
    public void ownSetStartingWeapon(String type) { ownBodies.setStartingWeapon(type); }
    public String ownStartingWeapon() { return ownBodies.startingWeapon(); }
    public String ownAutoEquip(String id) { return ownBodies.autoEquip(id); }
    public String ownDebugPlayerItem(Object player, String type) {
        if (player == null || type == null) return "no-player";
        try {
            Object inv = player.getClass().getMethod("getInventory").invoke(player);
            inv.getClass().getMethod("AddItem", String.class).invoke(inv, type);
            return type;
        } catch (Throwable error) { return "add-error=" + error.getClass().getSimpleName(); }
    }

    public String ownGiveWeapon(String id, Object player) { return ownBodies.giveWeaponFromPlayer(id, player); }
    public String ownLastWeapon(String id) { return ownBodies.lastWeapon(id); }

    public String ownEquipmentSnapshot() { return ownBodies.equipmentSnapshot(); }
    public void ownRestoreEquipment(String text) { ownBodies.restoreEquipment(text); }

    public String ownSetProfession(String id, String profession) {
        ownLayer.profession(id, profession);
        return ownLayer.profession(id);
    }

    public String ownProfession(String id) { return ownLayer.profession(id); }
    public String ownProfessions(String id) { return ownLayer.professions(id); }
    public String ownAssignProfessions(String id, String csv) { return ownLayer.assignProfessions(id, csv); }
    public String ownBlueprintStart(int width, int height) { return ownLayer.blueprintStart(width, height); }
    public String ownBlueprintTick(int workers) { return ownLayer.blueprintTick(workers); }
    public String ownBlueprintState() { return ownLayer.blueprintState(); }

    public String ownFarmTick(boolean water) { return ownLayer.farmTick(water); }
    public String ownFarmState() { return ownLayer.farmState(); }

    public String ownBaseSnapshot() { return ownLayer.baseSnapshot(); }
    public void ownRestoreBase(String text) { ownLayer.restoreBase(text); }

    public String ownBaseStock() { return ownLayer.baseStock(); }

    public String ownBaseProduce(String profession) { return ownLayer.baseProduce(profession); }
    public String ownBaseCook() { return ownLayer.cook(); }
    public String ownBaseDaily(int residents) { return ownLayer.baseDaily(residents); }
    public String ownBaseDefense() { return ownLayer.baseDefense(); }

    public String ownHealFromBase(String id) {
        if (ownLayer.agent(id) == null) return "no-agent";
        String healed = ownLayer.healWounded();
        if (healed.startsWith("healed")) ownLayer.heal(id, 0.5);
        return healed;
    }

    public String ownBaseHeal() { return ownLayer.healWounded(); }
    public String ownBaseTake(int food, int water, int meds) { return ownLayer.takeStock(food, water, meds); }
    public String ownBaseScavenge(int roll) { return ownLayer.scavenge(roll); }
    public String ownBaseCraftAmmo() { return ownLayer.craftAmmo(); }
    public String ownBaseBoilWater() { return ownLayer.boilWater(); }
    public String ownBaseFish() { return ownLayer.fish(); }
    public String ownBaseHunt() { return ownLayer.hunt(); }
    public String ownBaseLaundry() { return ownLayer.laundry(); }
    public String ownBaseRepair() { return ownLayer.repair(); }
    public String ownBaseAlarm(int level) { return ownLayer.alarm(level); }
    public String ownBaseJournal() { return ownLayer.journalText(); }
    public String ownBaseOps() { return ownLayer.baseOps(); }
    public int[] ownGuardShifts(int guards, int shifts) { return personal.knox.npc.own.OwnNpcLayer.guardShifts(guards, shifts); }
    public int ownConstructionMaterials(int w, int h) { return ownLayer.constructionMaterials(w, h); }
    public int ownPurifyWater(int raw) { return ownLayer.purifyWater(raw); }
    public int ownFishYield(boolean rod) { return ownLayer.fishYield(rod); }
    public int ownHuntYield(boolean rifle) { return ownLayer.huntYield(rifle); }
    public int ownStorageCap(String type) { return ownLayer.storageCap(type); }
    public int ownGeneratorHours(int fuel) { return ownLayer.generatorHours(fuel); }
    public int ownVehicleFuelCost(int km) { return ownLayer.vehicleFuelCost(km); }
    public String ownEquipmentTemplate(String profession) { return ownLayer.equipmentTemplate(profession); }
    public String ownBaseRule(String key, int value) { return ownLayer.rule(key, value); }
    public String ownBaseRules() { return ownLayer.rules(); }
    public int ownBaseRuleCount() { return ownLayer.ruleCount(); }
    public String ownBaseSleep() { return ownLayer.sleep(); }
    public String ownBaseShower() { return ownLayer.shower(); }
    public String ownBaseFortifyWindow() { return ownLayer.fortifyWindow(); }
    public String ownBaseWatchtower() { return ownLayer.watchtower(); }
    public String ownBaseRadio() { return ownLayer.radioBroadcast(); }
    public String ownBaseTrade() { return ownLayer.trade(); }
    public String ownBaseTrain() { return ownLayer.train(); }
    public String ownBaseGuardDuty() { return ownLayer.guardDuty(); }
    public String ownBaseRepairClothing() { return ownLayer.repairClothing(); }
    public String ownBaseCookRecipe() { return ownLayer.cookRecipe(); }
    public String ownBuildFacility(String kind) { return ownLayer.buildFacility(kind); }
    public String ownFacilities() { return ownLayer.facilities(); }
    public String ownFacilityOutput() { return ownLayer.facilityOutput(); }
    public String ownFacilityTick() { return ownLayer.facilityTick(); }
    public String ownAlarmTick() { return ownLayer.alarmTick(); }
    public String ownAddWounded(int count) { return ownLayer.addWounded(count); }
    public String ownRotateWounded() { return ownLayer.rotateWounded(); }
    public String ownAssignMedic(boolean value) { return ownLayer.assignMedic(value); }
    public String ownGuardShift(int shift) { return ownLayer.guardShift(shift); }
    public String ownClearJournal() { return ownLayer.clearJournal(); }
    public String ownDemolishFacility(String kind) { return ownLayer.demolishFacility(kind); }
    public int ownFacilityCount() { return ownLayer.facilityCount(); }
    public String ownBaseStatus() { return ownLayer.baseStatus(); }
    public int ownFacilityCost(String kind) { return ownLayer.facilityCost(kind); }
    public String ownFacilityUpgrade(String kind) { return ownLayer.facilityUpgrade(kind); }
    public String ownFacilityList() { return ownLayer.facilityList(); }
    public String ownBaseReport() { return ownLayer.baseReport(); }
    public String ownAlifeAdvanced() {
        var template = personal.knox.alife.AlifeFactionRoster.find("patrol");
        var identity = personal.knox.alife.AlifeFactionIdentity.of("f1", template, 10);
        var tactics = personal.knox.alife.AlifeTactics.plan(8, 1, 60, 6, true);
        var radio = new personal.knox.alife.AlifeRadioChannel();
        radio.call("patrol", 3, 10);
        var aftermath = personal.knox.alife.AlifeAftermath.from(personal.knox.alife.AlifeBattle.resolve(8, 6, 10, 8, 7), 3);
        return "identity=" + identity.templateId() + " greeting=" + identity.greeting()
                + " tactics=" + tactics.stance() + " radio=" + radio.reinforcement(3, 10,
                personal.knox.alife.AlifeReinforcement.Kind.WALK)
                + " bodies=" + aftermath.bodies() + " loot=" + aftermath.food();
    }

    public String ownAlifeCampaign(int day, int seed) { return alife.campaignDay(day, seed, 8, 6, 10, "outpost1"); }

    public String ownAlifePolicy() {
        var encounter = personal.knox.alife.AlifeEncounterPolicy.decide(60, 30, true, 50);
        var regroup = personal.knox.alife.AlifeRegroup.plan(8, 3, 25, 0, 0);
        var reinforcement = personal.knox.alife.AlifeReinforcement.plan(120, true, false);
        var editor = new personal.knox.alife.AlifeSquadEditor("s1", "jacket", "rifle", "aggressive", "neutral");
        return "encounter=" + encounter + " retreat=" + regroup.retreat()
                + " ready=" + regroup.ready() + " reinforce=" + reinforcement.kind()
                + " editor=" + (personal.knox.alife.AlifeSquadEditor.parse(editor.export()) != null);
    }

    public String ownAlifePatrol() {
        return personal.knox.alife.AlifePatrolPlanner.summary(0, 0, 12) + " "
                + personal.knox.alife.AlifeSituation.summary(3);
    }

    public String ownAlifeOperation(int roll) {
        var log = new personal.knox.alife.AlifeEncounterLog(16);
        var radio = new personal.knox.alife.AlifeRadioSilence();
        radio.silence("outpost1", 1, 2);
        var result = personal.knox.alife.AlifeOperation.run(8, 6, 10, 8, roll, 2,
                "marauders", log, radio, "outpost1");
        return "winner=" + result.winner() + " loss=" + result.attackerLoss()
                + " wounded=" + result.wounded() + " retreat=" + result.retreat()
                + " " + result.radio() + " " + log.summary();
    }

    public String ownAlifeCoreDemo() {
        var squad = new personal.knox.alife.Squad("s1", "patrol", 8, 90, 4,
                personal.knox.alife.Squad.Task.PATROL, 0, 0, 0, 10, 10);
        var state = personal.knox.alife.AlifeSquadState.from(squad, "leader").route(5, 5).withMorale(80);
        var battle = personal.knox.alife.AlifeBattle.resolve(8, 6, 10, 8, 7);
        var log = new personal.knox.alife.AlifeEncounterLog(16);
        log.add(1, "marauders", 4, 4, "seen");
        var radio = new personal.knox.alife.AlifeRadioSilence();
        radio.silence("outpost1", 1, 2);
        return "leader=" + state.leader() + " route=" + state.route().size()
                + " winner=" + battle.winner() + " loss=" + battle.attackerLoss()
                + " encounters=" + log.size() + " roster=" + personal.knox.alife.AlifeFactionRoster.count()
                + " " + personal.knox.alife.AlifeLod.summary(4, 120, 10, 2)
                + " silent=" + radio.silent("outpost1", 2);
    }

    public String ownBaseHelp() { return ownLayer.baseHelp(); }
    public String ownResetBase() { return ownLayer.resetBase(); }
    public String ownBaseNap() { return ownLayer.nap(); }
    public String ownBaseCoffee() { return ownLayer.coffee(); }
    public String ownBaseRead() { return ownLayer.read(); }
    public String ownBaseMusic() { return ownLayer.music(); }
    public String ownBaseCards() { return ownLayer.cards(); }
    public String ownBaseShave() { return ownLayer.shave(); }
    public String ownBaseStretch() { return ownLayer.stretch(); }
    public String ownBaseCleanWeapon() { return ownLayer.cleanWeapon(); }
    public String ownBaseInspectArmor() { return ownLayer.inspectArmor(); }
    public String ownBasePlanDay() { return ownLayer.planDay(); }

    public boolean ownHasProfession(String profession) {
        if (profession == null) return false;
        for (var agent : ownLayer.agents())
            if (ownLayer.hasProfession(agent.id(), profession)) return true;
        return false;
    }

    public String ownBaseStaffing(int total, String priority) {
        var assignments = personal.knox.npc.own.OwnBaseStaffing.plan(total,
                personal.knox.npc.own.OwnBaseStaffing.parse(priority));
        var out = new StringBuilder();
        for (var assignment : assignments) out.append(assignment.profession()).append('=')
                .append(assignment.count()).append(';');
        return out.toString();
    }

    public String ownAlertPhrase(int roll) { return personal.knox.npc.own.OwnNpcAlert.phrase(roll); }

    public int ownArmedCount() { return ownBodies.armedCount(); }

    public int ownHolsterAll() {
        int count = 0;
        for (var agent : ownLayer.agents()) if (ownBodies.holster(agent.id()).startsWith("holstered ")) count++;
        return count;
    }

    public String ownAutoWeapon(String id, boolean threat) { return ownBodies.autoWeapon(id, threat); }

    public String ownHolster(String id) { return ownBodies.holster(id); }
    public String ownDraw(String id) { return ownBodies.draw(id); }
    public int ownDrawAll() {
        int count = 0;
        for (var agent : ownLayer.agents()) {
            if (ownBodies.draw(agent.id()).startsWith("drawn ")) count++;
        }
        return count;
    }

    public String ownGiveClothing(String id, Object player) { return ownBodies.giveClothingFromPlayer(id, player); }

    public String ownGiveItem(String id, Object player, String type) {
        return ownBodies.giveItemFromPlayer(id, player, type);
    }

    public String ownInventory(String id) { return ownBodies.inventorySummary(id); }
    public String ownEat(String id) {
        String result = ownBodies.consumeFood(id);
        if (result.startsWith("ate ")) {
            ownLayer.relieve(id, 0.5, 0);
            ownLayer.consumeSupply(id, 1);
        }
        System.out.println("[PersonalKnox][own-eat] " + id + " " + result + " needs=" + ownLayer.needs(id));
        return result;
    }

    public String ownDrink(String id) {
        String result = ownBodies.consumeDrink(id);
        if (result.startsWith("drank ")) {
            ownLayer.relieve(id, 0, 0.5);
            ownLayer.consumeSupply(id, 1);
        }
        System.out.println("[PersonalKnox][own-drink] " + id + " " + result + " needs=" + ownLayer.needs(id));
        return result;
    }

    public String ownNeeds(String id) { return ownLayer.needs(id); }
    public double ownHealth(String id) {
        var agent = ownLayer.agent(id);
        return agent == null ? 0 : agent.state().health();
    }
    public int ownSupplies(String id) { return ownLayer.supplies(id); }
    public String ownSupplyState(String id) { return ownBodies.supplyState(id, ownLayer); }
    public String ownBandage(String id) {
        String result = ownBodies.consumeBandage(id);
        if (result.startsWith("bandaged ")) ownLayer.heal(id, 15);
        System.out.println("[PersonalKnox][own-bandage] " + id + " " + result + " " + ownLayer.needs(id));
        return result;
    }

    public String ownDebugPlaceLoot(String id) { return ownBodies.debugPlaceLoot(id); }
    public String ownDebugLoot(String id) { return ownBodies.debugLoot(id, ownLayer); }
    public void ownDebugSupplies(String id, int amount) { ownLayer.debugSetSupplies(id, amount); }
    public void ownDebugSetNeeds(String id, double hunger, double thirst) { ownLayer.debugSetNeeds(id, hunger, thirst); }
    public String ownDebugItem(String id, String type) {
        Object body = ownBodies.body(id);
        if (body == null) return "no-body";
        try {
            Object inv = body.getClass().getMethod("getInventory").invoke(body);
            inv.getClass().getMethod("AddItem", String.class).invoke(inv, type);
            return type;
        } catch (Throwable error) { return "add-error=" + error.getClass().getSimpleName(); }
    }

    public String ownPrimaryItem(String id) {
        Object body = ownBodies.body(id);
        if (body == null) return "";
        try {
            Object item = body.getClass().getMethod("getPrimaryHandItem").invoke(body);
            return item == null ? "" : String.valueOf(item.getClass().getMethod("getFullType").invoke(item));
        } catch (Throwable error) { return ""; }
    }

    public String ownDismantleTick(String id, double seconds) { return ownBodies.tickDismantle(id, seconds); }



    public String ownBodyTools(String id) {
        var tools = ownBodies.tools(id);
        var text = new StringBuilder();
        for (var tool : tools) text.append(tool.name()).append(';');
        return text.toString();
    }


    public String ownTargetZSnapshot() {
        return personal.knox.npc.own.OwnNpcTargetZCodec.encode(ownLayer.targetZMap());
    }

    public int ownTargetZRestore(String text) {
        ownLayer.restoreTargetZ(personal.knox.npc.own.OwnNpcTargetZCodec.decode(text));
        return ownLayer.targetZMap().size();
    }



    public String ownOrder(String id, double x, double y) {
        return ownLayer.order(id, x, y).agentId();
    }

    private static String alifeTask(personal.knox.npc.NpcState.Task task) {
        return switch (task) {
            case FOLLOW, GUARD -> "REGROUP";
            case WORK, LOOT -> "SCAVENGE";
            case REST, IDLE -> "HOLD";
            case FIGHT -> "RAID";
            case FLEE -> "TRAVEL";
        };
    }

    public int ownTick(double hours, int budget) {
        int events = ownLayer.tick(hours, budget).size();
        for (var agent : ownLayer.agents()) {
            alifeSquad(agent.id(), agent.factionId(), 1, agent.state().health(), agent.supplies(),
                    alifeTask(agent.state().task()), agent.x(), agent.y(), agent.z(),
                    agent.targetX(), agent.targetY());
        }
        return events;
    }


    public String ownRuntimeTick(Object player, int members, int bodyLimit, int moveBudget, double radius) {
        if (player == null) return "no-player";
        try {
            double px = ((Number) player.getClass().getMethod("getX").invoke(player)).doubleValue();
            double py = ((Number) player.getClass().getMethod("getY").invoke(player)).doubleValue();
            double pz = ((Number) player.getClass().getMethod("getZ").invoke(player)).doubleValue();
            ownGroup("player-squad", "own", Math.max(1, members), px, py);
            ownSync(px, py, radius, Math.max(1, bodyLimit));
            ownFollowTarget3(px, py, pz);
            ownTick(0.01, 32);
            ownBodies.setMoveBudget(Math.max(1, Math.min(64, moveBudget)));
            int bodies = ownBodies.syncBudgeted(ownLayer);
            return "active=" + ownLayer.count() + " bodies=" + bodies;
        } catch (ReflectiveOperationException error) {
            return "player-probe-failed=" + error.getClass().getSimpleName();
        }
    }

    public int ownBodiesSync() { return ownBodies.sync(ownLayer); }
    public void ownSetMoveBudget(int value) { ownBodies.setMoveBudget(value); }
    public void ownSetSyncBudget(int value) { ownBodies.setSyncBudgetMs(value); }
    public void ownSetIdentityRussianFirst(boolean value) { ownBodies.setPreferRussianNames(value); }
    public double ownCombatDamage(int weaponLevel, int skill, int condition, int conditionMax, boolean ranged) {
        return personal.knox.npc.own.OwnNpcCombat.damage(weaponLevel, skill, condition, conditionMax, ranged);
    }

    public double ownCombatRange(int remoteRange, boolean scope, String weather) {
        return personal.knox.npc.own.OwnNpcCombat.engagementRange(remoteRange, scope, weather);
    }

    public String ownRetreatPoint(double x, double y, double ex, double ey, double desired) {
        var point = personal.knox.npc.own.OwnNpcCombat.retreatPoint(x, y, ex, ey, desired, 2);
        return String.format(java.util.Locale.ROOT, "x=%.2f,y=%.2f", point.x(), point.y());
    }

    public String ownBaseSet(double x, double y, double z) { return ownLayer.setBase(x, y, z); }
    public String ownBaseSummary() { return ownLayer.baseSummary(); }
    public int ownBaseGuardAll() { return ownLayer.guardBase(); }

    private Object debugZombie;

    public int ownGiveItems(String id, Object player) {
        Object body = ownBodies.body(id);
        if (body == null || player == null) return 0;
        try {
            Object from = body.getClass().getMethod("getInventory").invoke(body);
            Object to = player.getClass().getMethod("getInventory").invoke(player);
            Object items = from.getClass().getMethod("getItems").invoke(from);
            int size = (int) items.getClass().getMethod("size").invoke(items);
            int moved = 0;
            for (int i = size - 1; i >= 0; i--) {
                Object item = items.getClass().getMethod("get", int.class).invoke(items, i);
                if (item == null) continue;
                Object added = to.getClass().getMethod("AddItem",
                        Class.forName("zombie.inventory.InventoryItem")).invoke(to, item);
                if (added != null) {
                    from.getClass().getMethod("Remove",
                            Class.forName("zombie.inventory.InventoryItem")).invoke(from, item);
                    moved++;
                }
            }
            return moved;
        } catch (Throwable error) { return 0; }
    }

    public String ownDebugEquip(String id, String type, int ammo) {
        Object body = ownBodies.body(id);
        if (body == null) return "no-body";
        try {
            Object inventory = body.getClass().getMethod("getInventory").invoke(body);
            Object item = inventory.getClass().getMethod("AddItem", String.class).invoke(inventory, type);
            body.getClass().getMethod("setPrimaryHandItem", Class.forName("zombie.inventory.InventoryItem"))
                    .invoke(body, item);
            item.getClass().getMethod("setCurrentAmmoCount", int.class).invoke(item, ammo);
            return "equipped=" + type + " ammo=" + ammo;
        } catch (Throwable error) { return "equip-failed=" + error.getClass().getSimpleName(); }
    }

    public int ownDebugAmmo(String id) {
        Object body = ownBodies.body(id);
        if (body == null) return -1;
        try {
            Object item = body.getClass().getMethod("getPrimaryHandItem").invoke(body);
            if (item == null) return -1;
            return ((Number) item.getClass().getMethod("getCurrentAmmoCount").invoke(item)).intValue();
        } catch (Throwable error) { return -1; }
    }

    public String ownDebugZombie(String id) {
        Object body = ownBodies.body(id);
        if (body == null) return "no-body";
        try {
            Object world = Class.forName("zombie.iso.IsoWorld").getField("instance").get(null);
            Object cell = world.getClass().getMethod("getCell").invoke(world);
            Object square = body.getClass().getMethod("getCurrentSquare").invoke(body);
            Object zombieList = cell.getClass().getMethod("getZombieList").invoke(cell);
            int size = (int) zombieList.getClass().getMethod("size").invoke(zombieList);
            if (size == 0) return "no-zombies";
            Object zombie = null;
            for (int i = 0; i < size; i++) {
                Object candidate = zombieList.getClass().getMethod("get", int.class).invoke(zombieList, i);
                if (candidate != null && candidate.getClass().getName().equals("zombie.characters.IsoZombie")) {
                    zombie = candidate;
                    break;
                }
            }
            if (zombie == null) return "no-exact-zombie";
            Class<?> zombieClass = Class.forName("zombie.characters.IsoZombie");
            float x = ((Number) body.getClass().getMethod("getX").invoke(body)).floatValue() + 0.6f;
            float y = ((Number) body.getClass().getMethod("getY").invoke(body)).floatValue();
            float z = ((Number) body.getClass().getMethod("getZ").invoke(body)).floatValue();
            zombieClass.getMethod("setX", float.class).invoke(zombie, x);
            zombieClass.getMethod("setY", float.class).invoke(zombie, y);
            zombieClass.getMethod("setZ", float.class).invoke(zombie, z);
            zombieClass.getMethod("setCurrentSquare", Class.forName("zombie.iso.IsoGridSquare")).invoke(zombie, square);
            debugZombie = zombie;
            zombieClass.getMethod("setHealth", float.class).invoke(zombie, 100f);
            return "zombie=reused health=" + zombieHealth();
        } catch (Throwable error) {
            System.out.println("[PersonalKnox][own-combat] spawn failed: " + error);
            return "zombie-failed=" + error.getClass().getSimpleName();
        }
    }

    public double ownDebugZombieHealth() { return zombieHealth(); }

    public int ownDebugKillZombie() {
        if (debugZombie == null) return 0;
        try {
            debugZombie.getClass().getMethod("setHealth", float.class).invoke(debugZombie, 0f);
            return 1;
        } catch (Throwable error) { return 0; }
    }

    public int ownCleanCorpses(double radius) {
        Object player = getSpecificPlayerForCorpse();
        if (player == null) return 0;
        try {
            double px = ((Number) player.getClass().getMethod("getX").invoke(player)).doubleValue();
            double py = ((Number) player.getClass().getMethod("getY").invoke(player)).doubleValue();
            Object cell = player.getClass().getMethod("getCell").invoke(player);
            @SuppressWarnings("unchecked")
            var zombies = (java.util.List<Object>) cell.getClass().getMethod("getZombieList").invoke(cell);
            int removed = 0;
            for (int i = zombies.size() - 1; i >= 0 && removed < 32; i--) {
                Object zombie = zombies.get(i);
                if (zombie == null) continue;
                try {
                    boolean dead = (boolean) zombie.getClass().getMethod("isDead").invoke(zombie);
                    if (!dead) continue;
                    double x = ((Number) zombie.getClass().getMethod("getX").invoke(zombie)).doubleValue();
                    double y = ((Number) zombie.getClass().getMethod("getY").invoke(zombie)).doubleValue();
                    if (Math.hypot(x - px, y - py) > radius) continue;
                    zombie.getClass().getMethod("removeFromWorld").invoke(zombie);
                    zombie.getClass().getMethod("removeFromSquare").invoke(zombie);
                    zombies.remove(i);
                    removed++;
                } catch (Throwable ignored) { }
            }
            return removed;
        } catch (Throwable error) { return 0; }
    }

    private Object getSpecificPlayerForCorpse() {
        try { return Class.forName("zombie.characters.IsoPlayer").getMethod("getInstance").invoke(null); }
        catch (Throwable ignored) { return null; }
    }

    public int ownDebugZombieClear() {
        if (debugZombie == null) return 0;
        try {
            debugZombie.getClass().getMethod("setTarget", Class.forName("zombie.iso.IsoMovingObject"))
                    .invoke(debugZombie, new Object[]{null});
            debugZombie.getClass().getMethod("setHealth", float.class).invoke(debugZombie, 0f);
            debugZombie.getClass().getMethod("removeFromWorld").invoke(debugZombie);
            debugZombie.getClass().getMethod("removeFromSquare").invoke(debugZombie);
        } catch (Throwable ignored) { }
        debugZombie = null;
        return 1;
    }

    public String ownDebugAttack(String id) {
        ownBodies.debugAttack(id);
        return "health=" + zombieHealth();
    }

    private double zombieHealth() {
        if (debugZombie == null) return -1;
        try {
            return ((Number) debugZombie.getClass().getMethod("getHealth").invoke(debugZombie)).doubleValue();
        } catch (Throwable error) { return -1; }
    }

    public String ownRemove(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Invalid own NPC id");
        ownLayer.remove(id);
        ownBodies.sync(ownLayer);
        return id;
    }

    public String ownDebugSpawn(double x, double y, double z) {
        var event = ownLayer.debugSpawn(x, y, z);
        ownLayer.order(event.agentId(), personal.knox.npc.own.OwnNpcOrder.FOLLOW, x + 3, y);
        ownLayer.followTarget(x, y);
        return event.agentId();
    }

    public int ownDebugBodySync() { return ownBodies.sync(ownLayer); }
    public String ownCreateCompanion(Object player) {
        double[] p=personal.knox.npc.own.OwnNpcBodyAdapter.spawnPosition(player, ownLayer.count());
        String id=ownDebugSpawn(p[0],p[1],p[2]);
        ownLayer.order(id,personal.knox.npc.own.OwnNpcOrder.FOLLOW,p[0],p[1]);
        ownLayer.followTarget(p[0],p[1],p[2]);
        ownBodies.sync(ownLayer);
        return id;
    }
    public boolean ownBodyReady(String id) { return ownBodies.ready(id); }
    public String ownBodyInspect(String id) { return ownBodies.inspect(id); }
    /** Dev/game adapter hook: the real IsoPlayer body for an own agent, or null. */
    public Object ownBodyObject(String id) { return ownBodies.body(id); }

    public String ownBodyWorn(String id) { return ownBodies.worn(id); }


    public String ownStartCompanions(Object player, int fallbackCount) {
        if (player == null || fallbackCount < 0 || fallbackCount > 4)
            throw new IllegalArgumentException("Invalid starting companion request");
        int count = ownCompanionTraitCount(player);
        if (count == 0) count = fallbackCount;
        var ids = new java.lang.StringBuilder();
        for (int i = 0; i < count; i++) {
            String id = ownCreateCompanion(player);
            if (id == null || id.isBlank()) break;
            ids.append(id).append(';');
        }
        return ids.toString();
    }

    public int ownCompanionTraitCount(Object player) {
        if (player == null) return 0;
        try {
            Object traits = player.getClass().getMethod("getCharacterTraits").invoke(player);
            Object map = traits.getClass().getMethod("getTraits").invoke(traits);
            int count = 0;
            for (var entry : ((java.util.Map<?, ?>) map).entrySet()) {
                if (!Boolean.TRUE.equals(entry.getValue())) continue;
                String name = String.valueOf(entry.getKey().getClass().getMethod("getName").invoke(entry.getKey()));
                int marker = name.indexOf("Companions");
                if (marker < 0) continue;
                String suffix = name.substring(marker + "Companions".length());
                try {
                    int value = Integer.parseInt(suffix);
                    if (value >= 1 && value <= 4) count = Math.max(count, value);
                } catch (NumberFormatException ignored) { }
            }
            return count;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public int ownCount() { return ownLayer.count(); }

    public String ownId(int index) {
        var agents = ownLayer.agents();
        return index >= 0 && index < agents.size() ? agents.get(index).id() : "";
    }

    public double ownX(int index) {
        var agents = ownLayer.agents();
        return index >= 0 && index < agents.size() ? agents.get(index).x() : 0;
    }

    public double ownY(int index) {
        var agents = ownLayer.agents();
        return index >= 0 && index < agents.size() ? agents.get(index).y() : 0;
    }

    public double ownZ(int index) {
        var agents = ownLayer.agents();
        return index >= 0 && index < agents.size() ? agents.get(index).z() : 0;
    }


    public String ownPopulationSnapshot() {
        return personal.knox.npc.own.OwnNpcPopulationCodec.encode(ownPopulation.groups());
    }

    public int ownPopulationRestore(String text) {
        var groups = personal.knox.npc.own.OwnNpcPopulationCodec.decode(text);
        for (var group : groups) {
            if (ownPopulation.group(group.id()) == null) ownPopulation.add(group);
            else ownPopulation.update(group);
        }
        return ownPopulation.groupCount();
    }

    public String ownSnapshot() {
        return personal.knox.npc.own.OwnNpcCodec.encode(ownLayer.snapshotAgents());
    }

    public String ownBootstrap(Object player, int fallbackCount, String agents, String targetZ, String population) {
        return ownBootstrap(player, fallbackCount, agents, targetZ, population, false);
    }

    public String ownBootstrap(Object player, int fallbackCount, String agents, String targetZ, String population,
                               boolean matchPlayerClothes) {
        ownBodies.setMatchPlayerClothes(matchPlayerClothes);
        if (agents != null && !agents.isBlank()) {
            int restored = ownRestore(agents);
            if (targetZ != null && !targetZ.isBlank()) ownTargetZRestore(targetZ);
            if (population != null && !population.isBlank()) ownPopulationRestore(population);
            return "RESTORED:" + restored;
        }
        return "STARTED:" + ownStartCompanions(player, fallbackCount);
    }

    public int ownRestore(String text) {
        ownLayer.restore(personal.knox.npc.own.OwnNpcCodec.decode(text));
        return ownLayer.count();
    }



    private final personal.knox.alife.AlifeRuntime alife = new personal.knox.alife.AlifeRuntime();

    public void alifeSquad(String id, String faction, int members, double health, int supplies,
                           String task, double x, double y, double z, double tx, double ty) {
        alife.submit(id, faction, members, health, supplies, task, x, y, z, tx, ty);
    }

    public String alifeTick(long now, double x, double y, double z, int budget) {
        return alife.tick(now, x, y, z, budget);
    }

    public void alifeOutpost(String id, String faction, double x, double y, double z,
                             int garrison, int supplies, boolean radio) {
        alife.outpost(id, faction, x, y, z, garrison, supplies, radio);
    }

    public int alifeOutposts() { return alife.outpostCount(); }
    public String alifeSilence(String id, int day, int days) { return alife.silence(id, day, days); }
    public int ownAlifeMaterialize(Object player, int max) {
        if (player == null) return 0;
        try {
            double px = ((Number) player.getClass().getMethod("getX").invoke(player)).doubleValue();
            double py = ((Number) player.getClass().getMethod("getY").invoke(player)).doubleValue();
            double pz = ((Number) player.getClass().getMethod("getZ").invoke(player)).doubleValue();
            var near = new java.util.HashSet<String>();
            int count = 0;
            for (var squad : alife.squads()) {
                if (count >= Math.max(0, Math.min(8, max))) break;
                if (squad.distanceTo(px, py) > 80) continue;
                String id = "alife-" + squad.id();
                near.add(id);
                if (ownLayer.agent(id) == null) {
                    var state = new personal.knox.npc.NpcState(100, 0.3, 0.3, 0.3, 70, false, false,
                            personal.knox.npc.NpcState.Role.GUARD, personal.knox.npc.NpcState.Task.GUARD);
                    ownLayer.spawn(new personal.knox.npc.own.OwnNpcAgent(id, squad.factionId(),
                            squad.x(), squad.y(), pz, squad.targetX(), squad.targetY(), squad.supplies(), state));
                } else {
                    ownLayer.order(id, personal.knox.npc.own.OwnNpcOrder.GUARD, squad.targetX(), squad.targetY());
                }
                count++;
            }
            for (var agent : ownLayer.agents())
                if (agent.id().startsWith("alife-") && !near.contains(agent.id())) ownLayer.remove(agent.id());
            return count;
        } catch (Throwable error) { return 0; }
    }

    public String alifeRadioCall(String faction, int day, int standing) { return alife.radioCall(faction, day, standing); }
    public String alifeSetting(String key, int value) { return alife.setting(key, value); }
    public String alifeSettings() { return alife.settings(); }
    public String ownAlifeInteraction(int standing) {
        var r = personal.knox.alife.AlifePlayerInteraction.decide(standing, true, true);
        return "greeting=" + r.greeting() + " hostile=" + r.hostile()
                + " trade=" + personal.knox.alife.AlifePlayerInteraction.trade(standing, true)
                + " help=" + personal.knox.alife.AlifePlayerInteraction.help(standing, true)
                + " " + personal.knox.alife.AlifePlayerInteraction.tribute(1, 1, 1);
    }

    public String alifeDirector(int seed, int day) { return alife.director(seed, day); }
    public boolean alifeDirectorReady(int day) { return alife.directorReady(day); }
    public void alifeDirectorDay(int day) { alife.directorDay(day); }
    public String alifeAftermath() { return alife.aftermath(); }
    public int alifeStateCount() { return alife.stateCount(); }
    public int alifeEncounterCount() { return alife.encounterCount(); }
    public String alifeWorldSilence(String id, int day, int days) { return alife.silenceOutpost(id, day, days); }
    public boolean alifeWorldSilent(String id, int day) { return alife.outpostSilent(id, day); }
    public String alifeWorldOperation(int attackers, int defenders, int power, int roll, int day, String faction, String outpost) {
        return alife.operation(attackers, defenders, power, roll, day, faction, outpost);
    }
    public boolean alifeSilent(String id, int day) { return alife.silent(id, day); }
    public String alifeOperation(int attackers, int defenders, int power, int roll, int day, String faction, String outpost) {
        return alife.operation(attackers, defenders, power, power, roll, day, faction, outpost);
    }

    public void alifeEconomy(int food, int water, int meds, int ammo) {
        alife.economy(food, water, meds, ammo);
    }

    public String alifeEvents() { return alife.lastEvents(); }

    public int alifeSquads() { return alife.squadCount(); }



    public String npcTransferApi(Object npc) { return KnoxTransferProbe.inspect(npc); }

    public int barricadeMissing(int planks, boolean metal) {
        return CompanionBrain.barricadeMissing(planks, metal);
    }
    public int barricadeStockTarget(int missing, boolean nails) {
        return CompanionBrain.barricadeStockTarget(missing, nails);
    }
    public String barricadeStatus(int missing, boolean ownsTask, boolean danger, double idleHours) {
        return CompanionBrain.barricadeStatus(missing, ownsTask, danger, idleHours);
    }

    private PersonalKnoxAPI() {
        var loaded = new ArrayList<Dialogue>();
        try (var stream = PersonalKnoxAPI.class.getResourceAsStream("/personal-knox/dialogues.tsv")) {
            if (stream == null) throw new IllegalStateException("Missing dialogue resource");
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    var parts = line.split("\t", 4);
                    if (parts.length != 4) throw new IllegalArgumentException("Invalid dialogue row");
                    loaded.add(new Dialogue(Integer.parseInt(parts[0]), parts[1], parts[2], parts[3]));
                }
            }
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load PersonalKnox dialogues", error);
        }
        if (loaded.isEmpty()) throw new IllegalStateException("Empty dialogues");
        dialogues = List.copyOf(loaded);
    }

    public String condition(double morale, double relationship, double bravery) {
        return CompanionBrain.condition(morale, relationship, bravery);
    }

    public int chooseDialogue(String topic, double morale, double relationship, double bravery, int roll) {
        var condition = condition(morale, relationship, bravery);
        var matching = new ArrayList<Dialogue>();
        var fallback = new ArrayList<Dialogue>();
        for (var row : dialogues) {
            if (!row.topic.equals(topic)) continue;
            if (row.condition.equals(condition)) matching.add(row);
            if (row.condition.equals("default")) fallback.add(row);
        }
        var pool = matching.isEmpty() ? fallback : matching;
        return pool.isEmpty() ? -1 : pool.get(Math.floorMod(roll, pool.size())).id;
    }

    public String dialogueText(int id) {
        return dialogues.stream().filter(row -> row.id == id).map(Dialogue::text).findFirst().orElse("");
    }

    public String targetLevel(double x, double y, double z, double tx, double ty, double tz, double range) {
        return CompanionBrain.targetLevel(x, y, z, tx, ty, tz, range);
    }

    public boolean allowRetreat(boolean inSquad, boolean groupOrder) {
        return CompanionBrain.allowRetreat(inSquad, groupOrder);
    }

    public String baseSchedule(int hour, String current) {
        return CompanionBrain.baseSchedule(hour, current);
    }

    public String supplyPlan(int food, int water, int medicine, boolean weapon, boolean danger, boolean overloaded) {
        return CompanionBrain.supplyPlan(food, water, medicine, weapon, danger, overloaded);
    }

    public boolean supplyCategoryNeeded(String category, int food, int water, int medicine, boolean weapon) {
        return CompanionBrain.supplyCategoryNeeded(category, food, water, medicine, weapon);
    }

    public void resetWorldReactions() { reactions.reset(); }

    public String directionalReaction(String topic, double facingX, double facingY, double dx, double dy) {
        return ThreatDirection.topic(topic, facingX, facingY, dx, dy);
    }

    public boolean isCleanBandage(String itemType, boolean canBandage, boolean infected) {
        return CompanionBrain.cleanBandage(itemType, canBandage, infected);
    }

    public boolean isSupplyFood(double hungerChange, boolean canEat, boolean rotten, boolean burnt,
                                boolean dangerousUncooked, boolean cooked, boolean knownPoison) {
        return SupplyEligibility.food(hungerChange, canEat, rotten, burnt, dangerousUncooked, cooked, knownPoison);
    }

    public boolean isSupplyWater(double amount, boolean pureWater, boolean tainted) {
        return SupplyEligibility.water(amount, pureWater, tainted);
    }

    public String worldReactionTopic(String key, double nowMillis, double health, double playerHealth,
            boolean bleeding, int zombies, boolean rain, boolean dark, boolean corpses) {
        if (!Double.isFinite(nowMillis) || nowMillis < 0 || nowMillis > Long.MAX_VALUE) return "";
        return reactions.observe(key, (long) nowMillis, health, playerHealth, bleeding, zombies, rain, dark, corpses);
    }

    public String worldReactionWithNeeds(String key, double nowMillis, double health, double playerHealth,
            boolean bleeding, int zombies, boolean rain, boolean dark, boolean corpses,
            double hunger, double thirst, double fatigue) {
        if (!Double.isFinite(nowMillis) || nowMillis < 0 || nowMillis > Long.MAX_VALUE) return "";
        return reactions.observe(key, (long) nowMillis, health, playerHealth, bleeding, zombies,
                rain, dark, corpses, hunger, thirst, fatigue);
    }

    /** Java-side bridge for PK-MED-006; Lua only supplies inventory counts and game state. */
    public String mobilityPlan(double mobility, boolean legInjured, boolean bleeding, double pain,
                               boolean canWalk, boolean onSortie, String supplies, boolean hasTool,
                               boolean safe, boolean allowImprovised, int reserveMaterialsPercent) {
        if (supplies == null) throw new IllegalArgumentException("Missing supplies");
        var stock = new java.util.LinkedHashMap<String, Integer>();
        for (var part : supplies.split(";")) {
            if (part.isBlank()) continue;
            int split = part.indexOf('=');
            if (split <= 0 || split == part.length() - 1)
                throw new IllegalArgumentException("Invalid supply entry");
            String id = part.substring(0, split).trim();
            int count;
            try { count = Integer.parseInt(part.substring(split + 1).trim()); }
            catch (NumberFormatException error) { throw new IllegalArgumentException("Invalid supply count", error); }
            stock.merge(id, count, Integer::sum);
        }
        var patient = new personal.knox.medical.PatientMobilityAssessment(
                mobility, legInjured, bleeding, pain, canWalk, onSortie);
        var plan = personal.knox.medical.MedicalSupplyPlanner.plan(patient, stock, hasTool,
                new personal.knox.medical.MedicalSupplyPlanner.Policy(safe, allowImprovised,
                        reserveMaterialsPercent));
        var text = new StringBuilder("action=").append(plan.action().name())
                .append(" aid=").append(plan.aidId() == null ? "" : plan.aidId())
                .append(" projected=").append(plan.projectedMobility())
                .append(" reason=").append(plan.reason());
        if (!plan.cost().isEmpty()) {
            text.append(" cost=");
            boolean first = true;
            for (var entry : plan.cost().entrySet()) {
                if (!first) text.append(',');
                text.append(entry.getKey()).append('=').append(entry.getValue());
                first = false;
            }
        }
        return text.toString();
    }

    public String careAction(double nowMillis, double health, int bleedingParts, double pain,
            double thirst, double hunger, boolean busy, boolean danger,
            boolean medic, boolean drink, boolean eat, boolean armed) {
        if (!Double.isFinite(nowMillis) || nowMillis < 0 || nowMillis > Long.MAX_VALUE) return PlayerCare.NONE;
        return care.decide((long) nowMillis, health, bleedingParts, pain, thirst, hunger,
                busy, danger, medic, drink, eat, armed);
    }

    public void careDone(String action, double nowMillis) {
        if (Double.isFinite(nowMillis) && nowMillis >= 0 && nowMillis <= Long.MAX_VALUE)
            care.done(action, (long) nowMillis);
    }

    public void careUnavailable(String action, double nowMillis) {
        if (Double.isFinite(nowMillis) && nowMillis >= 0 && nowMillis <= Long.MAX_VALUE)
            care.unavailable(action, (long) nowMillis);
    }

    public void resetCare() { care.reset(); }

    public double itemObjection(int kind, double hunger, double thirst, double health,
                                int freeSlots, int ammo) {
        ItemNeedPolicy.Kind type = switch (kind) {
            case 0 -> ItemNeedPolicy.Kind.FOOD;
            case 1 -> ItemNeedPolicy.Kind.WATER;
            case 2 -> ItemNeedPolicy.Kind.MEDICAL;
            case 3 -> ItemNeedPolicy.Kind.AMMO;
            default -> ItemNeedPolicy.Kind.OTHER;
        };
        return ItemNeedPolicy.weight(type,
                new ItemNeedPolicy.State(hunger, thirst, health, freeSlots, ammo));
    }

    public String groupChoice(double attackWeight, double holdWeight, double retreatWeight, int leaderChoice) {
        if (!Double.isFinite(attackWeight + holdWeight + retreatWeight)
                || attackWeight < 0 || holdWeight < 0 || retreatWeight < 0) return "hold";
        var votes = List.of(
                new GroupDecision.Vote(GroupDecision.Choice.ATTACK, Math.max(0.01, attackWeight)),
                new GroupDecision.Vote(GroupDecision.Choice.HOLD, Math.max(0.01, holdWeight)),
                new GroupDecision.Vote(GroupDecision.Choice.RETREAT, Math.max(0.01, retreatWeight)));
        GroupDecision.Choice leader = switch (leaderChoice) {
            case 1 -> GroupDecision.Choice.ATTACK;
            case 2 -> GroupDecision.Choice.HOLD;
            case 3 -> GroupDecision.Choice.RETREAT;
            default -> null;
        };
        return GroupDecision.decide(votes, leader).name().toLowerCase(java.util.Locale.ROOT);
    }

    public double npcNeedRate(int choice) {
        return NpcNeeds.rate(choice);
    }

    public boolean helpAllowed(int frequency, double nowMillis, double lastAtMillis) {
        if (!Double.isFinite(nowMillis) || !Double.isFinite(lastAtMillis)
                || nowMillis < 0 || lastAtMillis < 0
                || nowMillis > Long.MAX_VALUE || lastAtMillis > Long.MAX_VALUE) return false;
        return HelpPolicy.allowed(frequency, (long) nowMillis, (long) lastAtMillis);
    }

    public double helpInterval(int frequency) {
        return HelpPolicy.interval(frequency);
    }

    public boolean upkeepNeedsRepair(int condition, int max) {
        return UpkeepPolicy.needsRepair(condition, max);
    }

    public int upkeepAmmo(int current, int magazine) {
        return UpkeepPolicy.ammoToAdd(current, magazine);
    }

    public boolean upkeepNeedsMagazine(int count) {
        return UpkeepPolicy.needsMagazine(count);
    }
}
