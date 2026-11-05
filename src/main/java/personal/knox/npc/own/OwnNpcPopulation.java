package personal.knox.npc.own;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Strategic population: groups exist without bodies; only active ones get agents. */
public final class OwnNpcPopulation {
    public record Group(String id, String factionId, Kind kind, int members,
                        double x, double y, double targetX, double targetY) {
        public enum Kind { PLAYER_SQUAD, FACTION_SQUAD, BASE, ROAMING }
        public Group {
            if (id == null || id.isBlank() || factionId == null || factionId.isBlank() || kind == null)
                throw new IllegalArgumentException("Missing group data");
            if (members < 0 || members > 100_000) throw new IllegalArgumentException("Invalid members");
            if (!Double.isFinite(x + y + targetX + targetY)) throw new IllegalArgumentException("Invalid position");
        }
        public Group moved(double nx, double ny) {
            return new Group(id, factionId, kind, members, nx, ny, targetX, targetY);
        }
    }

    private final Map<String, Group> groups = new LinkedHashMap<>();

    public void add(Group group) {
        if (group == null || groups.containsKey(group.id()))
            throw new IllegalArgumentException("Duplicate or missing group");
        groups.put(group.id(), group);
    }

    public void update(Group group) {
        if (group == null || !groups.containsKey(group.id()))
            throw new IllegalArgumentException("Unknown group");
        groups.put(group.id(), group);
    }

    public Group remove(String id) { return groups.remove(id); }
    public Group group(String id) { return groups.get(id); }
    public List<Group> groups() { return List.copyOf(groups.values()); }
    public int groupCount() { return groups.size(); }

    public int totalMembers() {
        int total = 0;
        for (var group : groups.values()) total += group.members();
        return total;
    }

    /** Moves strategic groups toward their targets; no bodies are created. */
    public int tick(double stepMeters, int budget) {
        if (!Double.isFinite(stepMeters) || stepMeters < 0 || budget < 0)
            throw new IllegalArgumentException("Invalid population tick");
        int moved = 0;
        for (var entry : new ArrayList<>(groups.entrySet())) {
            if (moved >= budget) break;
            var group = entry.getValue();
            double dx = group.targetX() - group.x();
            double dy = group.targetY() - group.y();
            double length = Math.hypot(dx, dy);
            if (length <= 1e-6) continue;
            double step = Math.min(stepMeters, length);
            groups.put(group.id(), group.moved(group.x() + dx / length * step, group.y() + dy / length * step));
            moved++;
        }
        return moved;
    }
}
