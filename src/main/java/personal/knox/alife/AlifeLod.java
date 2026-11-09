package personal.knox.alife;

/** Distance-based simulation LOD and bounded work budget. */
public final class AlifeLod {
    public enum Level { FULL, SIMPLIFIED, ABSTRACT }
    private AlifeLod() {}

    public static Level level(double distance) {
        if (distance <= 80) return Level.FULL;
        if (distance <= 300) return Level.SIMPLIFIED;
        return Level.ABSTRACT;
    }

    public static int processed(int squads, int budget, int perSquadMs) {
        if (squads <= 0 || budget <= 0 || perSquadMs <= 0) return 0;
        return Math.min(squads, budget / perSquadMs);
    }

    public static String summary(int squads, double distance, int budget, int perSquadMs) {
        return "lod=" + level(distance) + " squads=" + squads + " processed=" + processed(squads, budget, perSquadMs);
    }
}
