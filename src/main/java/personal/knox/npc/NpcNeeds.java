package personal.knox.npc;

/** Deterministic need decay for own NPC snapshots. */
public final class NpcNeeds {
    public static final double HUNGER_PER_HOUR = 0.012;
    public static final double THIRST_PER_HOUR = 0.018;
    public static final double FATIGUE_PER_HOUR = 0.010;

    private NpcNeeds() {}

    public static NpcState tick(NpcState state, double hours) {
        if (state == null || !Double.isFinite(hours) || hours < 0)
            throw new IllegalArgumentException("Invalid need tick");
        return state.withNeeds(
                clamp(state.hunger() + HUNGER_PER_HOUR * hours),
                clamp(state.thirst() + THIRST_PER_HOUR * hours),
                clamp(state.fatigue() + FATIGUE_PER_HOUR * hours));
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
