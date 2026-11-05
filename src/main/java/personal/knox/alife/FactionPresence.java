package personal.knox.alife;

/** A faction survives losing its last squad and may return at the map edge later. */
public final class FactionPresence {
    public record State(String factionId, int members, boolean hasBase, long lastActiveMillis) {
        public State {
            if (factionId == null || factionId.isBlank()) throw new IllegalArgumentException("Missing faction id");
            if (members < 0 || lastActiveMillis < 0) throw new IllegalArgumentException("Invalid faction state");
        }
    }

    private FactionPresence() {}

    public static State afterLoss(State state, long nowMillis) {
        if (state == null || nowMillis < 0) throw new IllegalArgumentException("Invalid loss");
        return new State(state.factionId(), 0, state.hasBase(), nowMillis);
    }

    public static boolean shouldRespawn(State state, long nowMillis, long delayMillis) {
        if (state == null || nowMillis < 0 || delayMillis <= 0) throw new IllegalArgumentException("Invalid respawn");
        return state.members() == 0 && nowMillis - state.lastActiveMillis() >= delayMillis;
    }

    public static State respawn(State state, int members, long nowMillis) {
        if (state == null || members <= 0 || nowMillis < 0) throw new IllegalArgumentException("Invalid respawn");
        return new State(state.factionId(), members, state.hasBase(), nowMillis);
    }
}
