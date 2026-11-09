package personal.knox.alife;

/** One deterministic A-Life day: situation, battle, regroup, patrol and radio. */
public final class AlifeCampaign {
    public record DayReport(int day, String situation, String winner, int attackerLoss, int defenderLoss,
                            boolean retreat, boolean regroup, String radio, String patrol) {}

    private AlifeCampaign() {}

    public static DayReport step(int day, int seed, int attackers, int defenders, int power,
                                 AlifeEncounterLog log, AlifeRadioSilence radio, String outpostId) {
        var situation = AlifeSituation.pick(seed);
        var battle = AlifeBattle.resolve(attackers, defenders, power, power, seed);
        if (log != null) log.add(day, "marauders", 0, 0, battle.winner());
        var regroup = AlifeRegroup.plan(attackers, battle.wounded(), battle.retreat() ? 20 : 60, 0, 0);
        String radioText = "none";
        if (radio != null && outpostId != null) radioText = radio.responsePatrol(outpostId, day);
        return new DayReport(day, situation.name(), battle.winner(), battle.attackerLoss(),
                battle.defenderLoss(), battle.retreat(), regroup.regroup(), radioText,
                AlifePatrolPlanner.summary(0, 0, 8));
    }
}
