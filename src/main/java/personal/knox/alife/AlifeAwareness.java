package personal.knox.alife;

/** Perception without knowledge through walls. */
public final class AlifeAwareness {
    private AlifeAwareness() {}

    public static boolean canSee(double distance, boolean blocked, int perception) {
        if (blocked || !Double.isFinite(distance) || distance < 0) return false;
        int range = Math.max(1, Math.min(120, perception));
        return distance <= range;
    }

    public static String summary(double distance, boolean blocked, int perception) {
        return "visible=" + canSee(distance, blocked, perception) + " range=" + perception;
    }
}
