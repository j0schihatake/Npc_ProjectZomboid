package personal.knox;

/** Standalone decisions; no Knox classes are copied, patched or bundled. */
public final class CompanionBrain {
    private CompanionBrain() {}

    public static String condition(double morale, double relationship, double bravery) {
        if (Double.isFinite(relationship) && relationship < 30) return "low_trust";
        if (Double.isFinite(morale) && morale < 0.3) return "low_morale";
        if (Double.isFinite(bravery) && bravery > 65) return "brave";
        return "default";
    }

    public static String targetLevel(double x, double y, double z, double tx, double ty, double tz, double range) {
        if (!Double.isFinite(range) || range < 0 || !Double.isFinite(x + y + z + tx + ty + tz)) return "unknown";
        if (Math.pow(x - tx, 2) + Math.pow(y - ty, 2) > range * range) return "out_of_range";
        if (tz < z) return "below";
        if (tz > z) return "above";
        return "same_floor";
    }

    /** A squad member never abandons the squad on its own. */
    public static boolean allowRetreat(boolean inSquad, boolean groupRetreatOrder) {
        return !inSquad || groupRetreatOrder;
    }

    public static String supplyPlan(int food, int water, int medicine, boolean weapon, boolean danger, boolean overloaded) {
        if (danger || overloaded) return "wait";
        if (food < 2 || water < 1 || medicine < 2) return "essentials";
        if (!weapon) return "weapons";
        return "none";
    }

    public static boolean cleanBandage(String itemType, boolean canBandage, boolean infected) {
        if (!canBandage || infected || itemType == null) return false;
        String name = itemType.toLowerCase(java.util.Locale.ROOT);
        return !name.contains("dirty") && !name.contains("filthy") && !name.contains("bloody");
    }
}
