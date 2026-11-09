package personal.knox.alife;

/** Result left after an off-screen battle: bodies, loot and zombie attraction. */
public record AlifeAftermath(int bodies, int food, int meds, int ammo, int zombiesAttracted, int cleanupDay) {
    public AlifeAftermath {
        if (bodies < 0 || food < 0 || meds < 0 || ammo < 0 || zombiesAttracted < 0)
            throw new IllegalArgumentException("Invalid aftermath");
    }

    public static AlifeAftermath from(AlifeBattle.Result result, int day) {
        int bodies = Math.max(0, result.attackerLoss() + result.defenderLoss());
        return new AlifeAftermath(bodies, bodies / 2, bodies / 4, bodies / 3, bodies / 2, day + 1);
    }

    public static AlifeAftermath from(AlifeOperation.Result result, int day) {
        int bodies = Math.max(0, result.attackerLoss() + result.defenderLoss());
        return new AlifeAftermath(bodies, bodies / 2, bodies / 4, bodies / 3, bodies / 2, day + 1);
    }
}
