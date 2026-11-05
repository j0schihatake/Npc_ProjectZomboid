package personal.knox;

/** Whether an enemy considers surrender under pressure. No game objects. */
public final class SurrenderPolicy {
    private SurrenderPolicy() {}

    public static boolean considers(int enemyHealth, int enemyCount, int playerFirearms,
                                    int playerAllies, double enemyMorale) {
        if (enemyHealth < 0 || enemyHealth > 100 || enemyCount < 0
                || playerFirearms < 0 || playerAllies < 0
                || !Double.isFinite(enemyMorale) || enemyMorale < 0 || enemyMorale > 1)
            throw new IllegalArgumentException("Invalid surrender state");
        if (enemyCount == 0) return false;
        double pressure = playerFirearms * 2.0 + playerAllies - enemyCount
                + (100 - enemyHealth) / 10.0 + (1 - enemyMorale) * 3;
        return pressure >= 4;
    }

    public static int relationAfterSpare(int standing) {
        return Diplomacy.adjust(standing, Diplomacy.Action.SPARE);
    }
}
