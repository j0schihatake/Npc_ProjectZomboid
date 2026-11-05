package personal.knox.alife;

/** Abstract assault outcome for a faction raid on a base. */
public final class AssaultResolver {
    public record Result(boolean attackersWin, int attackerLosses, int defenderLosses) {
        public Result {
            if (attackerLosses < 0 || defenderLosses < 0) throw new IllegalArgumentException("Invalid losses");
        }
    }

    private AssaultResolver() {}

    public static Result resolve(int attackers, double attackerGear,
                                 int defenders, double defenderGear, int roll) {
        if (attackers < RaidPolicy.MIN_SIZE || attackers > RaidPolicy.MAX_SIZE)
            throw new IllegalArgumentException("Invalid attackers");
        if (defenders < 0) throw new IllegalArgumentException("Invalid defenders");
        if (!Double.isFinite(attackerGear) || attackerGear < 0 || attackerGear > 1
                || !Double.isFinite(defenderGear) || defenderGear < 0 || defenderGear > 1)
            throw new IllegalArgumentException("Invalid gear");
        if (roll < 0 || roll > 99) throw new IllegalArgumentException("Invalid roll");

        double attackPower = attackers * (0.5 + attackerGear) + roll % 20;
        double defensePower = defenders * (0.5 + defenderGear) + 10;
        boolean win = attackPower > defensePower;
        double ratio = win ? defensePower / Math.max(1, attackPower) : attackPower / Math.max(1, defensePower);
        int attackerLosses = (int) Math.round(attackers * (win ? ratio * 0.35 : (0.5 + ratio * 0.4)));
        int defenderLosses = (int) Math.round(defenders * (win ? (0.5 + ratio * 0.4) : ratio * 0.35));
        return new Result(win, Math.min(attackers, Math.max(0, attackerLosses)),
                Math.min(defenders, Math.max(0, defenderLosses)));
    }
}
