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
        return events;
    }
}
