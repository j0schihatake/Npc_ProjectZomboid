package personal.knox.alife;

/** One off-screen A-Life operation: battle, memory and radio response. */
public final class AlifeOperation {
    public record Result(String winner, int attackerLoss, int defenderLoss, int wounded,
                         boolean retreat, boolean regroup, String radio) {}

    private AlifeOperation() {}

    public static Result run(int attackers, int defenders, int attackPower, int defensePower,
                             int roll, int day, String factionId, AlifeEncounterLog log,
                             AlifeRadioSilence radio, String outpostId) {
        var battle = AlifeBattle.resolve(attackers, defenders, attackPower, defensePower, roll);
        if (log != null) log.add(day, factionId, 0, 0, battle.winner());
        String radioText = "none";
        if (radio != null && outpostId != null && !outpostId.isBlank())
            radioText = radio.responsePatrol(outpostId, day);
        return new Result(battle.winner(), battle.attackerLoss(), battle.defenderLoss(),
                battle.wounded(), battle.retreat(), battle.regroup(), radioText);
    }
}
