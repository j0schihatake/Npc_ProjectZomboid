package personal.knox.npc.own;

/** Data-only base defense assignment: windows first, then reserve, unarmed hide. */
public final class OwnBaseDefense {
    public record Plan(int windows, int reserve, int hidden, int unarmedWithoutCover) {}
    private OwnBaseDefense() {}

    public static Plan plan(int armed, int unarmed, int windows) {
        int a = Math.max(0, armed);
        int u = Math.max(0, unarmed);
        int w = Math.max(0, windows);
        int windowDefenders = Math.min(a, w);
        int reserve = Math.max(0, a - windowDefenders);
        return new Plan(windowDefenders, reserve, u, Math.max(0, u - w));
    }
}
