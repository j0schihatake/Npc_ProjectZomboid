package personal.knox;

/** Pure equipment upkeep decisions: durability and ammunition reserves. */
public final class UpkeepPolicy {
    private UpkeepPolicy() {}

    public static boolean needsRepair(int condition, int max) {
        return condition >= 0 && max > 0 && condition < max;
    }

    /** Keep at most three magazines of reserve; the player still reloads normally. */
    public static int ammoToAdd(int current, int magazine) {
        if (current < 0 || magazine <= 0 || magazine > 1000) return 0;
        int target = Math.min(300, magazine * 3);
        return Math.max(0, target - current);
    }

    public static boolean needsMagazine(int count) {
        return count <= 0;
    }
}
