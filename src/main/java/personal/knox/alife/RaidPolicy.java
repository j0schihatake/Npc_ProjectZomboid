package personal.knox.alife;

/** Raid group sizes and cadence; bounds are exposed to mod settings. */
public final class RaidPolicy {
    public static final int MIN_SIZE = 10;
    public static final int MAX_SIZE = 30;

    public record Parameters(int minSize, int maxSize, int intervalMinutes) {
        public Parameters {
            if (minSize < MIN_SIZE || maxSize > MAX_SIZE || minSize > maxSize)
                throw new IllegalArgumentException("Invalid raid size");
            if (intervalMinutes <= 0) throw new IllegalArgumentException("Invalid raid interval");
        }
    }

    private RaidPolicy() {}

    /** Deterministic roll: 0..99 maps into the configured range. */
    public static int size(int roll, Parameters parameters) {
        if (parameters == null || roll < 0 || roll > 99) throw new IllegalArgumentException("Invalid raid roll");
        int span = parameters.maxSize() - parameters.minSize() + 1;
        return parameters.minSize() + roll % span;
    }

    public static boolean due(long nowMillis, long lastRaidMillis, Parameters parameters) {
        if (parameters == null || nowMillis < 0 || lastRaidMillis < 0)
            throw new IllegalArgumentException("Invalid raid clock");
        return nowMillis - lastRaidMillis >= parameters.intervalMinutes() * 60_000L;
    }
}
