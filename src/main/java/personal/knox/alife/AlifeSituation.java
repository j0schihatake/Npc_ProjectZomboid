package personal.knox.alife;

import java.util.List;

/** Described A-Life situations with deterministic seeded selection. */
public final class AlifeSituation {
    public enum Kind { PATROL, HIGHWAY_AMBUSH, CHECKPOINT, BROKEN_CAR, OCCUPIED_HOUSE, SNIPER, PUNITIVE_RAID, BASE_RAID }
    public static final List<Kind> ALL = List.of(Kind.values());
    private AlifeSituation() {}

    public static Kind pick(int seed) { return ALL.get(Math.floorMod(seed, ALL.size())); }

    public static String summary(int seed) {
        Kind kind = pick(seed);
        return "situation=" + kind + " raid=" + (kind == Kind.BASE_RAID || kind == Kind.PUNITIVE_RAID);
    }
}
