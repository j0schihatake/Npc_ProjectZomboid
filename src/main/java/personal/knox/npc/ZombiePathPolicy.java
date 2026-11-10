package personal.knox.npc;

/** B42 zombie traversal policy: no rope, rope allowed, or deterministic random. */
public final class ZombiePathPolicy {
    public enum Mode { NO_ROPE, WITH_ROPE, RANDOM }

    private volatile Mode mode = Mode.NO_ROPE;

    public void setMode(String value) {
        if (value == null) return;
        mode = switch (value.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "WITH_ROPE", "ROPE", "ALL" -> Mode.WITH_ROPE;
            case "RANDOM", "MIXED" -> Mode.RANDOM;
            default -> Mode.NO_ROPE;
        };
    }

    public Mode mode() { return mode; }
    public String status() { return "mode=" + mode.name().toLowerCase(java.util.Locale.ROOT); }

    public boolean ropeAllowed(String id, boolean crawling) {
        if (crawling) return false;
        return switch (mode) {
            case NO_ROPE -> false;
            case WITH_ROPE -> true;
            case RANDOM -> Math.floorMod(String.valueOf(id).hashCode(), 2) == 0;
        };
    }
}
