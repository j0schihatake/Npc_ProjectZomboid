package personal.knox.npc.own;

import personal.knox.npc.NpcState;

/** Promotes strategic group members into active bodies, bounded by the active limit. */
public final class OwnNpcPromotion {
    private OwnNpcPromotion() {}

    public static int promote(OwnNpcPopulation population, OwnNpcLayer layer,
                              String groupId, int wanted) {
        if (population == null || layer == null || groupId == null || wanted < 0)
            throw new IllegalArgumentException("Invalid promotion");
        var group = population.group(groupId);
        if (group == null) throw new IllegalArgumentException("Unknown group");
        int available = OwnNpcLayer.MAX_NPCS - layer.count();
        int count = Math.min(Math.min(wanted, available), group.members());
        int created = 0;
        for (int i = 1; i <= count; i++) {
            String id = group.id() + "#" + i;
            if (layer.agent(id) != null) continue;
            var state = new NpcState(100, 0.2, 0.2, 0.2, 0.7, false, false,
                    NpcState.Role.SCAVENGER, NpcState.Task.FOLLOW);
            layer.spawn(new OwnNpcAgent(id, group.factionId(), group.x(), group.y(), 0,
                    group.x(), group.y(), 2, state));
            created++;
        }
        return created;
    }

    /** Removes active bodies of a group above {@code leaveActive}; strategic population is unchanged. */
    public static int demote(OwnNpcPopulation population, OwnNpcLayer layer,
                             String groupId, int leaveActive) {
        if (population == null || layer == null || groupId == null || leaveActive < 0)
            throw new IllegalArgumentException("Invalid demotion");
        if (population.group(groupId) == null) throw new IllegalArgumentException("Unknown group");
        var active = new java.util.ArrayList<String>();
        for (var agent : layer.agents()) {
            if (agent.id().startsWith(groupId + "#")) active.add(agent.id());
        }
        active.sort(String::compareTo);
        int remove = Math.max(0, active.size() - leaveActive);
        for (int i = 0; i < remove; i++) layer.remove(active.get(i));
        return remove;
    }
}
