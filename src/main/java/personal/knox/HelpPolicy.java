package personal.knox;

/** Frequency policy for future help situations. No game objects. */
public final class HelpPolicy {
    public static final long MINUTE = 60_000;

    private HelpPolicy() {}

    /** 1 = off, 2 = rare, 3 = normal, 4 = often. */
    public static long interval(int frequency) {
        return switch (frequency) {
            case 1 -> 0;
            case 2 -> 15 * MINUTE;
            case 3 -> 8 * MINUTE;
            case 4 -> 4 * MINUTE;
            default -> 0;
        };
    }

    public static boolean allowed(int frequency, long now, long lastAt) {
        if (now < 0 || lastAt < 0) return false;
        long interval = interval(frequency);
        return interval > 0 && (lastAt == 0 || now - lastAt >= interval);
    }
}
