package personal.knox.alife;

import java.util.LinkedHashMap;

/** Live bridge: Lua submits real squad snapshots, Java returns compact event counts. */
public final class AlifeRuntime {
    private final AlifeWorld world = new AlifeWorld(1);
    private long lastTick;
    private String lastSummary = "";
    private String lastEvents = "";

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

    public String lastSummary() { return lastSummary; }
    public String lastEvents() { return lastEvents; }
    public int squadCount() { return world.squadCount(); }
}
