package personal.knox.alife;

/** Retreat and regroup plan after losses; never abandon own members. */
public final class AlifeRegroup {
    public record Plan(boolean retreat, boolean regroup, double x, double y, int ready) {}
    private AlifeRegroup() {}

    public static Plan plan(int members, int wounded, int morale, double baseX, double baseY) {
        int alive = Math.max(0, members);
        boolean retreat = wounded * 2 >= Math.max(1, alive) || morale < 30;
        boolean regroup = alive > 0 && retreat;
        int ready = Math.max(0, alive - wounded);
        return new Plan(retreat, regroup, baseX, baseY, ready);
    }
}
