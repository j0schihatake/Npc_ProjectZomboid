package personal.knox.npc.own;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import personal.knox.npc.NpcNeeds;

/** Explicit, bounded, game-agnostic own NPC manager. */
public final class OwnNpcLayer {
    public static final int MAX_NPCS = 64;
    public static final double STEP_METERS = 2.0;
    private final Map<String, OwnNpcAgent> agents = new LinkedHashMap<>();
    private double needRate = 1;

    public void setNeedRate(double rate) {
        if (!Double.isFinite(rate) || rate < 0) throw new IllegalArgumentException("Invalid need rate");
        needRate = rate;
    }

    public int count() { return agents.size(); }
    public OwnNpcAgent agent(String id) { return agents.get(id); }
    public List<OwnNpcAgent> agents() { return List.copyOf(agents.values()); }

    public OwnNpcEvent spawn(OwnNpcAgent agent) {
        if (agent == null || agents.containsKey(agent.id()))
            throw new IllegalArgumentException("Duplicate or missing agent");
        if (agents.size() >= MAX_NPCS) throw new IllegalStateException("NPC budget exhausted");
        agents.put(agent.id(), agent);
        return new OwnNpcEvent(OwnNpcEvent.Type.SPAWNED, agent.id(), agent.x(), agent.y(), agent.z(), "spawn");
    }

    public OwnNpcEvent remove(String id) {
        var agent = agents.remove(id);
        if (agent == null) throw new IllegalArgumentException("Unknown agent");
        return new OwnNpcEvent(OwnNpcEvent.Type.REMOVED, id, agent.x(), agent.y(), agent.z(), "remove");
    }

    /** One bounded simulation step; returns at most {@code budget} events. */
    public List<OwnNpcEvent> tick(double hours, int budget) {
        if (!Double.isFinite(hours) || hours < 0 || budget < 0)
            throw new IllegalArgumentException("Invalid tick");
        var events = new ArrayList<OwnNpcEvent>();
        for (var entry : new ArrayList<>(agents.entrySet())) {
            if (events.size() >= budget) break;
            var agent = entry.getValue();
            var nextState = NpcNeeds.tick(agent.state(), hours, needRate);
            double dx = agent.targetX() - agent.x();
            double dy = agent.targetY() - agent.y();
            double length = Math.hypot(dx, dy);
            double nx = agent.x();
            double ny = agent.y();
            if (length > 1e-6) {
                double step = Math.min(STEP_METERS, length);
                nx += dx / length * step;
                ny += dy / length * step;
            }
            var updated = agent.moved(nx, ny, agent.z()).withState(nextState);
            agents.put(agent.id(), updated);
            events.add(new OwnNpcEvent(OwnNpcEvent.Type.NEEDS, agent.id(), nx, ny, agent.z(),
                    String.format(java.util.Locale.ROOT, "hunger=%.3f thirst=%.3f", nextState.hunger(), nextState.thirst())));
            if (length > STEP_METERS) {
                events.add(new OwnNpcEvent(OwnNpcEvent.Type.MOVED, agent.id(), nx, ny, agent.z(), "step"));
            }
        }
        return List.copyOf(events);
    }
}
