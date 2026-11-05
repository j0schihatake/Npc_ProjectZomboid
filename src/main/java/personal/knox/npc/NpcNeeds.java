package personal.knox.npc;

/** Deterministic need decay for own NPC snapshots. */
public final class NpcNeeds {
    public static final double HUNGER_PER_HOUR = 0.012;
    public static final double THIRST_PER_HOUR = 0.018;
    public static final double FATIGUE_PER_HOUR = 0.010;

    private NpcNeeds() {}

    public static NpcState tick(NpcState state, double hours) {
        return tick(state, hours, 1);
    }

    /** 1..5 maps to very slow, slow, normal, fast, very fast. */
    public static double rate(int choice) {
        return switch (choice) {
            case 1 -> 0.25;
            case 2 -> 0.5;
            case 3 -> 1;
            case 4 -> 2;
            case 5 -> 4;
            default -> 1;
        };
    }

    public static NpcState tick(NpcState state, double hours, double rate) {
        if (state == null || !Double.isFinite(hours) || hours < 0
                || !Double.isFinite(rate) || rate < 0)
            throw new IllegalArgumentException("Invalid need tick");
        return state.withNeeds(
                clamp(state.hunger() + HUNGER_PER_HOUR * hours * rate),
                clamp(state.thirst() + THIRST_PER_HOUR * hours * rate),
                clamp(state.fatigue() + FATIGUE_PER_HOUR * hours * rate));
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
