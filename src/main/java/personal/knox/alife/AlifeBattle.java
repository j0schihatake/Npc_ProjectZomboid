package personal.knox.alife;

/** Off-screen battle resolution without spawning bodies. */
public final class AlifeBattle {
    public record Result(int attackerLoss, int defenderLoss, int wounded, boolean retreat, boolean regroup, String winner) {}
    private AlifeBattle() {}

    public static Result resolve(int attackers, int defenders, int attackPower, int defensePower, int roll) {
        int a = Math.max(0, Math.min(64, attackers));
        int d = Math.max(0, Math.min(64, defenders));
        if (a == 0 || d == 0) return new Result(0, 0, 0, false, false, d == 0 ? "attacker" : "defender");
        int swing = Math.floorMod(roll, 21) - 10;
        int attackScore = attackPower + a * 2 + swing;
        int defenseScore = defensePower + d * 2;
        boolean attackerWins = attackScore >= defenseScore;
        int loserLoss = Math.max(1, (attackerWins ? d : a) / 2);
        int winnerLoss = Math.max(0, (attackerWins ? a : d) / 4);
        int wounded = Math.max(1, loserLoss / 2);
        boolean retreat = !attackerWins;
        return new Result(attackerWins ? winnerLoss : loserLoss, attackerWins ? loserLoss : winnerLoss,
                wounded, retreat, retreat, attackerWins ? "attacker" : "defender");
    }
}
