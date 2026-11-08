package personal.knox.npc.own;

import java.util.ArrayList;
import java.util.ArrayDeque;
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
    private final Map<String, Double> targetZ = new LinkedHashMap<>();
    private final List<String> schedule = new ArrayList<>();
    private final Map<String, Double> lastNeedClock = new LinkedHashMap<>();
    private final ArrayDeque<OwnNpcEvent> pendingEvents = new ArrayDeque<>();
    private int cursor;
    private double needClock;
    private double followX, followY;
    private double needRate = 1;

    public void setNeedRate(double rate) {
        if (!Double.isFinite(rate) || rate < 0) throw new IllegalArgumentException("Invalid need rate");
        needRate = rate;
    }

    public int count() { return agents.size(); }
    public OwnNpcAgent agent(String id) { return agents.get(id); }
    public List<OwnNpcAgent> agents() { return List.copyOf(agents.values()); }

    /** Catch up deferred need time for persistence only; normal body sync stays cheap. */
    public List<OwnNpcAgent> snapshotAgents() {
        return agents.values().stream().map(agent -> {
            double hours=needClock-lastNeedClock.get(agent.id());
            return hours==0 ? agent : agent.withState(NpcNeeds.tick(agent.state(),hours));
        }).toList();
    }

    public OwnNpcEvent spawn(OwnNpcAgent agent) {
        if (agent == null || agents.containsKey(agent.id()))
            throw new IllegalArgumentException("Duplicate or missing agent");
        if (agents.size() >= MAX_NPCS) throw new IllegalStateException("NPC budget exhausted");
        agents.put(agent.id(), agent);
        schedule.add(agent.id());
        lastNeedClock.put(agent.id(),needClock);
        return new OwnNpcEvent(OwnNpcEvent.Type.SPAWNED, agent.id(), agent.x(), agent.y(), agent.z(), "spawn");
    }

    public OwnNpcEvent debugSpawn(double x, double y, double z) {
        int index=1;
        while(agents.containsKey("debug#"+index)) index++;
        String id="debug#"+index;
        var state = new personal.knox.npc.NpcState(100, 0.2, 0.2, 0.2, 0.7,
                false, false, personal.knox.npc.NpcState.Role.SCAVENGER,
                personal.knox.npc.NpcState.Task.FOLLOW);
        var event=spawn(new OwnNpcAgent(id, "own", x, y, z, x + 3, y, 2, state));
        targetZ.put(id, z);
        return event;
    }

    public void restore(List<OwnNpcAgent> restored) {
        if (restored == null) throw new IllegalArgumentException("Missing agents");
        if (restored.size() > MAX_NPCS) throw new IllegalArgumentException("Too many agents");
        var validated=new LinkedHashMap<String,OwnNpcAgent>();
        for (var agent : restored) {
            if(agent==null || validated.putIfAbsent(agent.id(),agent)!=null)
                throw new IllegalArgumentException("Null or duplicate restored agent");
        }
        agents.clear();agents.putAll(validated);
        schedule.clear();schedule.addAll(validated.keySet());
        orders.clear();targetZ.clear();lastNeedClock.clear();pendingEvents.clear();
        cursor=0;needClock=0;followX=0;followY=0;
        for(var agent : restored) {
            lastNeedClock.put(agent.id(),0.0);
            targetZ.put(agent.id(),agent.z());
        }
    }

    public OwnNpcEvent order(String id, OwnNpcOrder order, double tx, double ty) {
        if (order == null) throw new IllegalArgumentException("Missing order");
        var event = order(id, tx, ty);
        orders.put(id, order);
        var agent = agents.get(id);
        var task = switch (order) {
            case FOLLOW -> personal.knox.npc.NpcState.Task.FOLLOW;
            case HOLD -> personal.knox.npc.NpcState.Task.IDLE;
            case GUARD -> personal.knox.npc.NpcState.Task.GUARD;
            case LOOT -> personal.knox.npc.NpcState.Task.LOOT;
            case REST -> personal.knox.npc.NpcState.Task.REST;
        };
        agents.put(id, agent.withState(agent.state().withTask(task)));
        return event;
    }

    public void followTarget(double x, double y) {
        if (!Double.isFinite(x + y)) throw new IllegalArgumentException("Invalid follow target");
        followX = x;
        followY = y;
    }

    public void followTarget(double x, double y, double z) {
        followTarget(x, y);
        if (!Double.isFinite(z)) throw new IllegalArgumentException("Invalid follow floor");
        targetZ.put("__follow__", z);
    }

    public java.util.Map<String, Double> targetZMap() { return new LinkedHashMap<>(targetZ); }

    public void restoreTargetZ(java.util.Map<String, Double> values) {
        if (values == null) throw new IllegalArgumentException("Missing target floors");
        targetZ.clear();
        targetZ.putAll(values);
    }

    public double targetZ(String id) {
        return targetZ.getOrDefault(id, targetZ.getOrDefault("__follow__", 0.0));
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

    private double baseX, baseY, baseZ;
    private boolean hasBase;

    public synchronized String setBase(double x, double y, double z) {
        if (!Double.isFinite(x + y + z)) throw new IllegalArgumentException("Invalid base");
        baseX = x; baseY = y; baseZ = z; hasBase = true;
        return String.format(java.util.Locale.ROOT, "base=%.1f,%.1f,%.1f", x, y, z);
    }

    public synchronized String baseSummary() {
        if (!hasBase) return "base=none";
        return String.format(java.util.Locale.ROOT, "base=%.1f,%.1f,%.1f", baseX, baseY, baseZ);
    }

    public synchronized int guardBase() {
        if (!hasBase) throw new IllegalStateException("No base");
        int count = 0;
        for (var agent : agents.values()) {
            orders.put(agent.id(), OwnNpcOrder.GUARD);
            agents.put(agent.id(), agent.withTarget(baseX, baseY));
            count++;
        }
        return count;
    }

    public String needs(String id) {
        var agent = agent(id);
        if (agent == null) return "";
        return String.format(java.util.Locale.ROOT, "hunger=%.3f thirst=%.3f fatigue=%.3f",
                agent.state().hunger(), agent.state().thirst(), agent.state().fatigue());
    }

    public void relieve(String id, double hunger, double thirst) {
        var agent = agent(id);
        if (agent == null) return;
        var state = agent.state();
        agents.put(id, agent.withState(state.withNeeds(
                Math.max(0, state.hunger() - hunger), Math.max(0, state.thirst() - thirst), state.fatigue())));
    }

    public void debugSetNeeds(String id, double hunger, double thirst) {
        var agent = agent(id);
        if (agent == null) return;
        var state = agent.state();
        agents.put(id, agent.withState(state.withNeeds(
                Math.max(0, Math.min(1, hunger)), Math.max(0, Math.min(1, thirst)), state.fatigue())));
    }

    public void heal(String id, double amount) {
        var agent = agent(id);
        if (agent == null) return;
        var state = agent.state();
        agents.put(id, agent.withState(state.withHealth(Math.min(100, state.health() + amount)).withBleeding(false)));
    }

    private final java.util.Map<String, double[]> groupMissions = new java.util.HashMap<>();

    public void groupMission(String groupId, double x, double y, double z) {
        groupMissions.put(groupId, new double[]{x, y, z});
    }

    public double[] groupMission(String groupId) {
        return groupMissions.get(groupId);
    }

    public int supplies(String id) {
        var agent = agent(id);
        return agent == null ? 0 : agent.supplies();
    }

    public void consumeSupply(String id, int amount) {
        if (amount <= 0) return;
        var agent = agent(id);
        if (agent == null) return;
        agents.put(id, agent.withSupplies(Math.max(0, agent.supplies() - amount)));
    }

    public void debugSetSupplies(String id, int amount) {
        var agent = agent(id);
        if (agent == null) return;
        agents.put(id, agent.withSupplies(Math.max(0, amount)));
    }

    public OwnNpcEvent addSupplies(String id, int amount) {
        if (amount < 0) throw new IllegalArgumentException("Invalid supplies");
        var agent = agents.get(id);
        if (agent == null) throw new IllegalArgumentException("Unknown agent");
        var updated = agent.withSupplies(agent.supplies() + amount);
        agents.put(id, updated);
        return new OwnNpcEvent(OwnNpcEvent.Type.NEEDS, id, updated.x(), updated.y(), updated.z(), "supplies");
    }

    public OwnNpcEvent damage(String id, double amount) {
        if (!Double.isFinite(amount) || amount <= 0) throw new IllegalArgumentException("Invalid damage");
        var agent = agents.get(id);
        if (agent == null) throw new IllegalArgumentException("Unknown agent");
        double health = Math.max(0, agent.state().health() - amount);
        if (health <= 0) {
            var removed = remove(id);
            return new OwnNpcEvent(OwnNpcEvent.Type.DIED, id, removed.x(), removed.y(), removed.z(), "dead");
        }
        agents.put(id, agent.withHealth(health));
        return new OwnNpcEvent(OwnNpcEvent.Type.NEEDS, id, agent.x(), agent.y(), agent.z(), "damage");
    }

    public OwnNpcEvent remove(String id) {
        var agent=agents.get(id);
        if(agent==null) throw new IllegalArgumentException("Unknown agent");
        int index=schedule.indexOf(id);
        agents.remove(id);schedule.remove(index);
        orders.remove(id);targetZ.remove(id);lastNeedClock.remove(id);
        pendingEvents.removeIf(event -> event.agentId().equals(id));
        if(index<cursor) cursor--;
        cursor=schedule.isEmpty() ? 0 : cursor%schedule.size();
        return new OwnNpcEvent(OwnNpcEvent.Type.REMOVED, id, agent.x(), agent.y(), agent.z(), "remove");
    }

    /** Rotate agents fairly, emitting at most budget events. Deferred NPCs retain
     * elapsed need time; an extra movement notification is delivered next call. */
    public List<OwnNpcEvent> tick(double hours, int budget) {
        if (!Double.isFinite(hours) || hours < 0 || budget < 0)
            throw new IllegalArgumentException("Invalid tick");
        double nextClock=needClock+hours*needRate;
        if(!Double.isFinite(nextClock)) throw new IllegalArgumentException("Need time overflow");
        needClock=nextClock;
        if(budget==0 || agents.isEmpty()) return List.of();
        var events = new ArrayList<OwnNpcEvent>();
        while(!pendingEvents.isEmpty() && events.size()<budget) events.add(pendingEvents.removeFirst());
        int processed=0;
        while(processed<schedule.size() && events.size()<budget) {
            String id=schedule.get(cursor);
            cursor=(cursor+1)%schedule.size();
            processed++;
            var agent=agents.get(id);
            var order = orders.getOrDefault(agent.id(), OwnNpcOrder.HOLD);
            if (order == OwnNpcOrder.FOLLOW) {
                double followDistance = Math.hypot(followX - agent.x(), followY - agent.y());
                agent = followDistance > 2.5
                        ? agent.withTarget(followX, followY)
                        : agent.withTarget(agent.x(), agent.y());
                agents.put(agent.id(), agent);
            } else if (order == OwnNpcOrder.HOLD || order == OwnNpcOrder.REST) {
                agent = agent.withTarget(agent.x(), agent.y());
                agents.put(agent.id(), agent);
            }
            var nextState = NpcNeeds.tick(agent.state(),needClock-lastNeedClock.get(id));
            lastNeedClock.put(id,needClock);
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
            if (nextState.bleeding() && supplies > 0) {
                nextState = nextState.withBleeding(false);
                supplies--;
            }
            if (!nextState.bleeding() && !nextState.downed()
                    && nextState.hunger() < 0.6 && nextState.thirst() < 0.6) {
                nextState = nextState.withHealth(Math.min(100, nextState.health() + 2.0 * hours));
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
                var moved=new OwnNpcEvent(OwnNpcEvent.Type.MOVED,agent.id(),nx,ny,agent.z(),"step");
                if(events.size()<budget) events.add(moved);
                else pendingEvents.addLast(moved);
            }
        }
        return List.copyOf(events);
    }
}
