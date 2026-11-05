package personal.knox.alife;

/** One faction raid with 10-30 people and its state. */
public record Raid(String id, String attackerFactionId, TargetKind targetKind, String targetId,
                   int size, long startedAtMillis, State state) {
    public enum TargetKind { BASE, RESOURCE_NODE, CARAVAN }
    public enum State { MARCHING, BATTLE, WON, LOST, RETREAT }

    public Raid {
        if (id == null || id.isBlank() || attackerFactionId == null || attackerFactionId.isBlank()
                || targetId == null || targetId.isBlank() || targetKind == null || state == null)
            throw new IllegalArgumentException("Missing raid data");
        if (size < RaidPolicy.MIN_SIZE || size > RaidPolicy.MAX_SIZE)
            throw new IllegalArgumentException("Invalid raid size");
        if (startedAtMillis < 0) throw new IllegalArgumentException("Invalid raid start");
    }

    public Raid withState(State next) {
        if (next == null) throw new IllegalArgumentException("Missing raid state");
        return new Raid(id, attackerFactionId, targetKind, targetId, size, startedAtMillis, next);
    }
}
