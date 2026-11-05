package personal.knox;

/** Readiness of reserves, using observable item state and the NPC's poison knowledge. */
public final class SupplyEligibility {
    private SupplyEligibility() {}

    public static boolean food(double hungerChange, boolean canEat, boolean rotten, boolean burnt,
                               boolean dangerousUncooked, boolean cooked, boolean knownPoison) {
        return Double.isFinite(hungerChange) && hungerChange < 0 && canEat && !rotten && !burnt
                && (!dangerousUncooked || cooked) && !knownPoison;
    }

    public static boolean water(double amount, boolean pureWater, boolean tainted) {
        return Double.isFinite(amount) && amount > 0 && pureWater && !tainted;
    }
}
