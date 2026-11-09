package personal.knox.alife;

/** Warning before shooting, chance to leave, otherwise fight. */
public final class AlifeEncounterPolicy {
    public enum Outcome { WARN, LEAVE, FIGHT }
    private AlifeEncounterPolicy() {}

    public static Outcome decide(int hostility, int distance, boolean armed, int roll) {
        int r = Math.floorMod(roll, 100);
        if (hostility < 20) return Outcome.WARN;
        if (distance > 40 && !armed) return Outcome.LEAVE;
        if (hostility < 50 && r < 35) return Outcome.WARN;
        if (hostility < 70 && r < 20) return Outcome.LEAVE;
        return Outcome.FIGHT;
    }
}
