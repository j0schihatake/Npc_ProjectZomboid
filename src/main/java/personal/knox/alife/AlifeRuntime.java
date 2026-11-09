package personal.knox.alife;

import java.util.LinkedHashMap;

/** Live bridge: Lua submits real squad snapshots, Java returns compact event counts. */
public final class AlifeRuntime {
    private final AlifeWorld world = new AlifeWorld(1);
    private long lastTick;
    private String lastSummary = "";
    private String lastEvents = "";
    private boolean economyAdded;
    private final AlifeEncounterLog encounters = new AlifeEncounterLog(64);
    private final AlifeRadioSilence radios = new AlifeRadioSilence();

    public void submit(String squadId, String factionId, int members, double health, int supplies,
                       String task, double x, double y, double z, double targetX, double targetY) {
        Squad.Task parsed;
        try { parsed = Squad.Task.valueOf(task.toUpperCase(java.util.Locale.ROOT)); }
        catch (RuntimeException invalid) { parsed = Squad.Task.REGROUP; }
        var squad = new Squad(squadId, factionId, members, health, supplies, parsed, x, y, z, targetX, targetY);
        if (world.squad(squadId) == null) world.addSquad(squad);
        else world.updateSquad(squad);
    }

    public String tick(long nowMillis, double px, double py, double pz, int budget) {
        lastTick = Math.max(lastTick, nowMillis);
        var events = world.tick(lastTick, px, py, pz, budget);
        var counts = new LinkedHashMap<String, Integer>();
        for (var event : events) counts.merge(event.type().name(), 1, Integer::sum);
        lastSummary = counts.toString();
        var text = new StringBuilder();
        int written = 0;
        for (var event : events) {
            if (event.type() == AlifeEvent.Type.NEAR_SQUAD) continue;
            if (written++ >= 8) break;
            text.append(event.type().name()).append(':').append(event.squadId()).append(';');
        }
        lastEvents = text.toString();
        return lastSummary;
    }

    /** Seed the own faction stock once from the real player inventory. */
    public void economy(int food, int water, int meds, int ammo) {
        if (economyAdded) return;
        var stock = new FactionEconomy(1);
        stock.add(FactionEconomy.Resource.FOOD, Math.max(0, food));
        stock.add(FactionEconomy.Resource.WATER, Math.max(0, water));
        stock.add(FactionEconomy.Resource.MEDS, Math.max(0, meds));
        stock.add(FactionEconomy.Resource.AMMO, Math.max(0, ammo));
        world.addFactionEconomy("own", stock);
        economyAdded = true;
    }

    public void outpost(String id, String factionId, double x, double y, double z,
                        int garrison, int supplies, boolean radio) {
        var outpost = new Outpost(id, factionId, Outpost.Kind.CAMP, x, y, z, garrison, supplies, radio);
        if (world.outpost(id) == null) world.addOutpost(outpost);
        else world.updateOutpost(outpost);
    }

    public int outpostCount() { return world.outpostCount(); }

    public String campaignDay(int day, int seed, int attackers, int defenders, int power, String outpostId) {
        var report = AlifeCampaign.step(day, seed, attackers, defenders, power, encounters, radios, outpostId);
        return "day=" + report.day() + " situation=" + report.situation()
                + " winner=" + report.winner() + " loss=" + report.attackerLoss()
                + " retreat=" + report.retreat() + " " + report.radio();
    }

    public String silence(String outpostId, int day, int days) { return radios.silence(outpostId, day, days); }
    public boolean silent(String outpostId, int day) { return radios.silent(outpostId, day); }

    public String operation(int attackers, int defenders, int attackPower, int defensePower,
                            int roll, int day, String factionId, String outpostId) {
        var result = AlifeOperation.run(attackers, defenders, attackPower, defensePower, roll, day,
                factionId, encounters, radios, outpostId);
        return "winner=" + result.winner() + " loss=" + result.attackerLoss()
                + " wounded=" + result.wounded() + " " + result.radio() + " " + encounters.summary();
    }

    public String silenceOutpost(String id, int day, int days) { return world.silenceOutpost(id, day, days); }
    public boolean outpostSilent(String id, int day) { return world.outpostSilent(id, day); }
    public java.util.List<Squad> squads() { return world.squads(); }
    public int stateCount() { return world.stateCount(); }
    public int encounterCount() { return world.encounters().size(); }
    public String operation(int attackers, int defenders, int power, int roll, int day, String faction, String outpost) {
        return world.operation(attackers, defenders, power, roll, day, faction, outpost);
    }
    public String radioCall(String faction, int day, int standing) { return world.radioCall(faction, day, standing); }
    public String aftermath() { return world.aftermath(); }

    public String lastSummary() { return lastSummary; }
    public String lastEvents() { return lastEvents; }
    public int squadCount() { return world.squadCount(); }
}
