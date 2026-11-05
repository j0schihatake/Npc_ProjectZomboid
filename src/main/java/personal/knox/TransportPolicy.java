package personal.knox;

/** How a wounded companion can move. No game objects. */
public final class TransportPolicy {
    public enum Mode { WALK, SUPPORT, CARRY }

    private TransportPolicy() {}

    public static Mode mode(double health, boolean legInjured, boolean bleeding, double fatigue) {
        if (!Double.isFinite(health) || health < 0 || health > 100
                || !Double.isFinite(fatigue) || fatigue < 0 || fatigue > 1)
            throw new IllegalArgumentException("Invalid transport state");
        if (health < 25 || (legInjured && health < 50) || (bleeding && health < 35)) return Mode.CARRY;
        if (legInjured || bleeding || health < 60 || fatigue >= 0.80) return Mode.SUPPORT;
        return Mode.WALK;
    }

    public static boolean needsHelper(Mode mode) {
        return mode != null && mode != Mode.WALK;
    }

    public static double speedFactor(Mode mode) {
        return switch (mode) {
            case WALK -> 1.0;
            case SUPPORT -> 0.55;
            case CARRY -> 0.25;
        };
    }
}
