package personal.knox.alife;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Deterministic, bounded simulation. The adapter submits squad snapshots and
 * receives events; near squads are left to the game, far squads are simplified.
 */
public final class AlifeWorld {
    public static final double NEAR_RADIUS = 80;
    public static final double FAR_STEP = 8;

    private final Random random;
    private final Map<String, Squad> squads = new LinkedHashMap<>();
    private final Map<String, Outpost> outposts = new LinkedHashMap<>();
    private final Map<String, FactionPresence.State> presence = new LinkedHashMap<>();
    private final Map<String, FactionEconomy> economies = new LinkedHashMap<>();
    private final Map<String, Long> lastExpand = new LinkedHashMap<>();
    private final Map<String, ResourceNode> nodes = new LinkedHashMap<>();
    private long respawnDelayMillis = 600_000;
    private final Relations relations = new Relations();
    private long now;

    public AlifeWorld(long seed) { random = new Random(seed); }

    public Relations relations() { return relations; }
    public int squadCount() { return squads.size(); }
    public Squad squad(String id) { return squads.get(id); }
    public List<Squad> squads() { return List.copyOf(squads.values()); }
    public int outpostCount() { return outposts.size(); }
    public Outpost outpost(String id) { return outposts.get(id); }
    public List<Outpost> outposts() { return List.copyOf(outposts.values()); }

    public void addOutpost(Outpost outpost) {
        if (outpost == null) throw new IllegalArgumentException("Missing outpost");
        if (outposts.containsKey(outpost.id())) throw new IllegalArgumentException("Duplicate outpost");
        outposts.put(outpost.id(), outpost);
    }

    public Outpost removeOutpost(String id) { return outposts.remove(id); }

    public void setRespawnDelayMillis(long value) {
        if (value <= 0) throw new IllegalArgumentException("Invalid respawn delay");
        respawnDelayMillis = value;
    }

    public void addResourceNode(ResourceNode node) {
        if (node == null || nodes.containsKey(node.id()))
            throw new IllegalArgumentException("Duplicate or missing resource node");
        nodes.put(node.id(), node);
    }

    public ResourceNode resourceNode(String id) { return nodes.get(id); }
    public java.util.List<ResourceNode> resourceNodes() { return java.util.List.copyOf(nodes.values()); }

    public AlifeEvent captureResourceNode(String nodeId, String factionId, long time) {
        var node = nodes.get(nodeId);
        if (node == null) throw new IllegalArgumentException("Unknown resource node");
        nodes.put(nodeId, node.withController(factionId));
        return new AlifeEvent(AlifeEvent.Type.NODE_CAPTURED, nodeId, factionId,
                node.x(), node.y(), node.z(), "captured");
    }

    public void addFactionEconomy(String factionId, FactionEconomy economy) {
        if (factionId == null || economy == null || economies.containsKey(factionId))
            throw new IllegalArgumentException("Duplicate or missing faction economy");
        economies.put(factionId, economy);
    }

    public FactionEconomy factionEconomy(String factionId) { return economies.get(factionId); }

    public void addFactionPresence(FactionPresence.State state) {
        if (state == null || presence.containsKey(state.factionId()))
            throw new IllegalArgumentException("Duplicate or missing faction presence");
        presence.put(state.factionId(), state);
    }

    public FactionPresence.State factionPresence(String factionId) {
        return presence.get(factionId);
    }

    public void loseFactionSquad(String factionId, long nowMillis) {
        var state = presence.get(factionId);
        if (state == null) throw new IllegalArgumentException("Unknown faction");
        presence.put(factionId, FactionPresence.afterLoss(state, nowMillis));
    }

    public java.util.List<String> respawnReady(long nowMillis, long delayMillis) {
        var ready = new java.util.ArrayList<String>();
        for (var entry : presence.entrySet()) {
            if (FactionPresence.shouldRespawn(entry.getValue(), nowMillis, delayMillis))
                ready.add(entry.getKey());
        }
        return java.util.List.copyOf(ready);
    }

    public void respawnFaction(String factionId, int members, long nowMillis) {
        var state = presence.get(factionId);
        if (state == null) throw new IllegalArgumentException("Unknown faction");
        presence.put(factionId, FactionPresence.respawn(state, members, nowMillis));
    }

    public boolean canDispatch(String outpostId) {
        var outpost = outposts.get(outpostId);
        return outpost != null && outpost.canDispatch();
    }

    public void addSquad(Squad squad) {
        if (squad == null) throw new IllegalArgumentException("Missing squad");
        if (squads.containsKey(squad.id())) throw new IllegalArgumentException("Duplicate squad");
        squads.put(squad.id(), squad);
    }

    public Squad removeSquad(String id) { return squads.remove(id); }

    public List<AlifeEvent> tick(long time, double px, double py, double pz, int budget) {
        if (time < 0 || budget < 0) throw new IllegalArgumentException("Invalid tick");
        if (!Double.isFinite(px + py + pz)) throw new IllegalArgumentException("Invalid player position");
        if (time < now) throw new IllegalArgumentException("Time moved backwards");
        now = time;
        relations.decay(now);
        var events = new ArrayList<AlifeEvent>();
        int operations = 0;
        for (var entry : new ArrayList<>(squads.entrySet())) {
            if (operations >= budget) break;
            operations++;
            var squad = entry.getValue();
            if (squad.distanceTo(px, py) <= NEAR_RADIUS) {
                events.add(new AlifeEvent(AlifeEvent.Type.NEAR_SQUAD, squad.id(), squad.factionId(),
                        squad.x(), squad.y(), squad.z(), squad.task().name()));
                continue;
            }
            double dx = squad.targetX() - squad.x();
            double dy = squad.targetY() - squad.y();
            double length = Math.hypot(dx, dy);
            double nx = squad.x();
            double ny = squad.y();
            if (length > 1e-6) {
                double step = Math.min(FAR_STEP, length);
                nx += dx / length * step;
                ny += dy / length * step;
            }
            int supplies = Math.max(0, squad.supplies() - 1);
            var updated = squad.moved(nx, ny, squad.z()).withSupplies(supplies);
            squads.put(squad.id(), updated);
            events.add(new AlifeEvent(AlifeEvent.Type.FAR_MOVE, squad.id(), squad.factionId(),
                    nx, ny, squad.z(), squad.task().name()));
            if (supplies == 0) {
                events.add(new AlifeEvent(AlifeEvent.Type.FAR_RETURN, squad.id(), squad.factionId(),
                        nx, ny, squad.z(), "no_supplies"));
            } else if (squad.task() == Squad.Task.PATROL && random.nextInt(100) < 10) {
                events.add(new AlifeEvent(AlifeEvent.Type.FAR_ENCOUNTER, squad.id(), squad.factionId(),
                        nx, ny, squad.z(), "patrol"));
            }
        }
        for (var entry : new ArrayList<>(outposts.entrySet())) {
            var outpost = entry.getValue();
            if (!outpost.radio() || outpost.supplies() <= 0) continue;
            int supplies = outpost.supplies() - 1;
            boolean radio = supplies > 0;
            outposts.put(outpost.id(), outpost.withSupplies(supplies).withRadio(radio));
            if (!radio) {
                events.add(new AlifeEvent(AlifeEvent.Type.OUTPOST_SILENT, outpost.id(), outpost.factionId(),
                        outpost.x(), outpost.y(), outpost.z(), "radio_off"));
            }
        }
        for (var entry : economies.entrySet()) {
            var economy = entry.getValue();
            if (economy.tick(1, 1)) {
                events.add(new AlifeEvent(AlifeEvent.Type.FACTION_STARVING, entry.getKey(), entry.getKey(),
                        px, py, pz, "supplies"));
            }
            long last = lastExpand.getOrDefault(entry.getKey(), 0L);
            boolean ready = !lastExpand.containsKey(entry.getKey()) || time - last >= 3_600_000L;
            if (economy.canExpand() && ready) {
                economy.consume(FactionEconomy.Resource.FOOD, 20);
                economy.consume(FactionEconomy.Resource.WATER, 10);
                economy.consume(FactionEconomy.Resource.MATERIALS, 10);
                lastExpand.put(entry.getKey(), time);
                events.add(new AlifeEvent(AlifeEvent.Type.FACTION_EXPAND, entry.getKey(), entry.getKey(),
                        px, py, pz, "base"));
            }
        }
        for (var factionId : respawnReady(time, respawnDelayMillis)) {
            respawnFaction(factionId, 1, time);
            events.add(new AlifeEvent(AlifeEvent.Type.FACTION_RETURN, factionId, factionId,
                    px, py, pz, "edge"));
        }
        return events;
    }
}
