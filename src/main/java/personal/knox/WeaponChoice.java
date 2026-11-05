package personal.knox;

/** Own, constant-time equipment policy. No game objects or inventory mutations. */
public final class WeaponChoice {
    public enum Role { MELEE, RANGED }
    public enum Decision { LEAVE, EQUIP, UPGRADE, RESERVE }

    /** actionTime is a comparable positive time estimate within one weapon role. */
    public record Profile(Role role, double minDamage, double maxDamage, double actionTime,
                          int condition, int conditionMax, double weight, double range,
                          int skill, boolean ammunitionAvailable) {}

    private WeaponChoice() {}

    private static boolean known(Profile weapon) {
        return !(weapon == null || weapon.role == null || weapon.condition < 0 || weapon.conditionMax <= 0
                || weapon.condition > weapon.conditionMax || weapon.skill < 0 || weapon.skill > 10
                || !Double.isFinite(weapon.minDamage) || !Double.isFinite(weapon.maxDamage)
                || !Double.isFinite(weapon.actionTime) || !Double.isFinite(weapon.weight)
                || !Double.isFinite(weapon.range) || weapon.minDamage < 0
                || weapon.maxDamage < weapon.minDamage || weapon.maxDamage <= 0
                || weapon.actionTime <= 0 || weapon.weight < 0 || weapon.range < 0);
    }

    private static boolean unusable(Profile weapon) {
        return weapon.condition == 0 || weapon.role == Role.RANGED && (!weapon.ammunitionAvailable || weapon.range == 0);
    }

    /** A heuristic utility, not a claim of exact in-game DPS or hit probability. */
    public static double utility(Profile weapon) {
        if (!known(weapon) || unusable(weapon)) return 0;
        double quality = .35 + .65 * weapon.condition / (double) weapon.conditionMax;
        double proficiency = weapon.role == Role.RANGED ? .25 + .075 * weapon.skill : .6 + .08 * weapon.skill;
        double reach = 1 + .02 * Math.min(weapon.range, weapon.role == Role.RANGED ? 30 : 5);
        double score = ((weapon.minDamage + weapon.maxDamage) / 2) / weapon.actionTime
                * quality * proficiency * reach / (1 + .08 * weapon.weight);
        return Double.isFinite(score) && score > 0 ? score : 0;
    }

    /**
     * The adapter must report a real compatible slot and revalidate it at execution.
     * requiredWeight includes storage changes to the old weapon, if any.
     * ammunitionAvailable includes compatible reserves/reload, not just a loaded chamber.
     */
    public static Decision decide(Profile candidate, Profile incumbent, boolean canReplaceIncumbent,
                                  boolean canEquipEmptyHands, boolean compatibleFreeSlot,
                                  double requiredWeight, double freeWeight, double minimumGain) {
        if (!Double.isFinite(requiredWeight) || !Double.isFinite(freeWeight)
                || !Double.isFinite(minimumGain) || requiredWeight < 0 || freeWeight < requiredWeight
                || minimumGain < 0 || minimumGain > 1) return Decision.LEAVE;
        double value = utility(candidate);
        if (value == 0) return Decision.LEAVE;
        if (canReplaceIncumbent && known(incumbent) && candidate.role == incumbent.role) {
            double oldValue = utility(incumbent);
            if (unusable(incumbent) || (oldValue > 0 && value > oldValue && value >= oldValue * (1 + minimumGain)))
                return Decision.UPGRADE;
        }
        if (canEquipEmptyHands) return Decision.EQUIP;
        if (compatibleFreeSlot) return Decision.RESERVE;
        return Decision.LEAVE;
    }
}
