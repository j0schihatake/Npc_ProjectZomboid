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
