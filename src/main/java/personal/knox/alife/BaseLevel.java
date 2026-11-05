package personal.knox.alife;

/** Seven base levels; 5+ are self-sufficient and visibly richer. */
public final class BaseLevel {
    public static final int MAX = 7;

    private BaseLevel() {}

    public static void require(int level) {
        if (level < 1 || level > MAX) throw new IllegalArgumentException("Invalid base level");
    }

    public static boolean selfSufficient(int level) {
        require(level);
        return level >= 5;
    }

    public static double externalNeed(int level) {
        require(level);
        return level >= 5 ? 0 : (5 - level) * 0.25;
    }

    public static int beds(int level) { require(level); return level + 1; }
    public static int workbenches(int level) { require(level); return Math.max(0, (level - 1) / 2); }
    public static int chests(int level) { require(level); return level * 2; }
    public static int gardens(int level) { require(level); return Math.max(0, level - 2); }
    public static int animalPens(int level) { require(level); return Math.max(0, (level - 3) / 2); }
}
