package personal.knox.alife;

import java.util.List;

/** Chooses A-Life situations with weights and a cooldown. */
public final class AlifeDirector {
    public record Choice(int day, AlifeSituation.Kind kind, boolean raid) {}
    private AlifeDirector() {}

    public static Choice choose(int seed, int day) {
        var kind = AlifeSituation.pick(seed);
        return new Choice(day, kind, kind == AlifeSituation.Kind.BASE_RAID || kind == AlifeSituation.Kind.PUNITIVE_RAID);
    }

    public static boolean ready(int lastDay, int day, int frequencyDays) {
        return lastDay < 0 || day - lastDay >= Math.max(1, frequencyDays);
    }

    public static List<AlifeSituation.Kind> all() { return AlifeSituation.ALL; }
}
