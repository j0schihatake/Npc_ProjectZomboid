package personal.knox;

/** Faction standing and diplomatic reactions for A-Life groups. No game objects. */
public final class Diplomacy {
    public enum Stance { ALLIED, NEUTRAL, TENSE, HOSTILE }
    public enum Action { GIFT, AID, TRADE, BETRAYAL, ATTACK }

    private Diplomacy() {}

    public static int adjust(int standing, Action action) {
        if (standing < -100 || standing > 100) throw new IllegalArgumentException("Invalid standing");
        if (action == null) throw new IllegalArgumentException("Missing action");
        int delta = switch (action) {
            case GIFT -> 10;
            case AID -> 20;
            case TRADE -> 5;
            case BETRAYAL -> -30;
            case ATTACK -> -40;
        };
        return Math.max(-100, Math.min(100, standing + delta));
    }

    public static Stance stance(int standing) {
        if (standing < -100 || standing > 100) throw new IllegalArgumentException("Invalid standing");
        if (standing >= 50) return Stance.ALLIED;
        if (standing >= 0) return Stance.NEUTRAL;
        if (standing >= -39) return Stance.TENSE;
        return Stance.HOSTILE;
    }

    /** Aid and joining are open to allies and neutrals, not to tense/hostile groups. */
    public static boolean acceptsAid(int standing) {
        return stance(standing) == Stance.ALLIED || stance(standing) == Stance.NEUTRAL;
    }

    public static boolean canJoin(int standing) {
        return acceptsAid(standing);
    }
}
