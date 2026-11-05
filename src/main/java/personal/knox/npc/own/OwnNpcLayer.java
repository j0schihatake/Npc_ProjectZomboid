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
    private final Map<String, OwnNpcOrder> orders = new LinkedHashMap<>();
    private double followX, followY;
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

    public OwnNpcEvent debugSpawn(double x, double y, double z) {
        int index = agents.size();
        String id = "debug#" + (index + 1);
        double ox = (index % 4) * 0.9;
        double oy = ((index / 4) % 4) * 0.9;
        var state = new personal.knox.npc.NpcState(100, 0.2, 0.2, 0.2, 0.7,
                false, false, personal.knox.npc.NpcState.Role.SCAVENGER,
                personal.knox.npc.NpcState.Task.FOLLOW);
        return spawn(new OwnNpcAgent(id, "own", x + ox, y + oy, z, x + ox + 3, y + oy, 2, state));
    }

    public void restore(List<OwnNpcAgent> restored) {
        if (restored == null) throw new IllegalArgumentException("Missing agents");
        if (restored.size() > MAX_NPCS) throw new IllegalArgumentException("Too many agents");
        agents.clear();
        for (var agent : restored) agents.put(agent.id(), agent);
    }

    public OwnNpcEvent order(String id, OwnNpcOrder order, double tx, double ty) {
        if (order == null) throw new IllegalArgumentException("Missing order");
        var event = order(id, tx, ty);
        orders.put(id, order);
        return event;
    }

    public void followTarget(double x, double y) {
        if (!Double.isFinite(x + y)) throw new IllegalArgumentException("Invalid follow target");
        followX = x;
        followY = y;
    }

    public OwnNpcOrder orderOf(String id) { return orders.getOrDefault(id, OwnNpcOrder.HOLD); }

    public OwnNpcEvent order(String id, double tx, double ty) {
        var agent = agents.get(id);
        if (agent == null) throw new IllegalArgumentException("Unknown agent");
        if (!Double.isFinite(tx + ty)) throw new IllegalArgumentException("Invalid target");
        var updated = agent.withTarget(tx, ty);
        agents.put(id, updated);
        return new OwnNpcEvent(OwnNpcEvent.Type.MOVED, id, updated.x(), updated.y(), updated.z(), "order");
    }

    public OwnNpcEvent remove(String id) {
        orders.remove(id);
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
            if (orders.getOrDefault(agent.id(), OwnNpcOrder.HOLD) == OwnNpcOrder.FOLLOW) {
                agent = agent.withTarget(followX, followY);
                agents.put(agent.id(), agent);
            }
            var nextState = NpcNeeds.tick(agent.state(), hours, needRate);
            int supplies = agent.supplies();
            if (nextState.hunger() >= 0.6 && supplies > 0) {
                nextState = nextState.withNeeds(Math.max(0, nextState.hunger() - 0.5),
                        nextState.thirst(), nextState.fatigue());
                supplies--;
            }
            if (nextState.thirst() >= 0.6 && supplies > 0) {
                nextState = nextState.withNeeds(nextState.hunger(),
                        Math.max(0, nextState.thirst() - 0.5), nextState.fatigue());
                supplies--;
            }
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
            var updated = agent.moved(nx, ny, agent.z()).withState(nextState).withSupplies(supplies);
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
