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







    public int ownActive() { return ownLayer.count(); }

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

    public int ownTick(double hours, int budget) {
        return ownLayer.tick(hours, budget).size();
    }

    public int ownBodiesSync() { return ownBodies.sync(ownLayer); }
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
