package personal.knox.alife;

/** Theft and permission rules on another faction's base. */
public final class TheftPolicy {
    private TheftPolicy() {}

    public static boolean isTheft(boolean owner, boolean permitted, boolean placing) {
        return !owner && !permitted && !placing;
    }

    /** Negative relation delta for taking without permission; repeat offences hurt more. */
    public static int relationDelta(boolean permitted, boolean returned, int previousOffences) {
        if (previousOffences < 0) throw new IllegalArgumentException("Invalid offence count");
        if (permitted) return 0;
        int base = returned ? -3 : -8;
        return base * (1 + previousOffences);
    }

    public static boolean escalatesToWar(int relation, int offences) {
        if (offences < 0) throw new IllegalArgumentException("Invalid offence count");
        return relation <= -40 || offences >= 3;
    }
}
