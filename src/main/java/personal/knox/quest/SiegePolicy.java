package personal.knox.quest;

/** Siege horde size and the one-third survival requirement. */
public final class SiegePolicy {
    private SiegePolicy() {}

    public static int hordeSize(int baseLevel) {
        if (baseLevel < 1 || baseLevel > 7) throw new IllegalArgumentException("Invalid base level");
        return 10 + baseLevel * 5;
    }

    /** Ceiling of one third; at least one survivor is required. */
    public static int requiredSurvivors(int groupSize) {
        if (groupSize <= 0) throw new IllegalArgumentException("Invalid group size");
        return Math.max(1, (groupSize + 2) / 3);
    }

    public static boolean succeeded(int survivors, int groupSize) {
        if (survivors < 0) throw new IllegalArgumentException("Invalid survivors");
        return survivors >= requiredSurvivors(groupSize);
    }
}
