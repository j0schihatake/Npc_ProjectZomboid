package personal.knox.npc.own;

/** Output of the own NPC layer; the game adapter decides how to show it. */
public record OwnNpcEvent(Type type, String agentId, double x, double y, double z, String detail) {
    public enum Type { SPAWNED, MOVED, NEEDS, REMOVED }

    public OwnNpcEvent {
        if (type == null || agentId == null || agentId.isBlank())
            throw new IllegalArgumentException("Missing event data");
        if (!Double.isFinite(x + y + z)) throw new IllegalArgumentException("Invalid position");
        detail = detail == null ? "" : detail;
    }
}
