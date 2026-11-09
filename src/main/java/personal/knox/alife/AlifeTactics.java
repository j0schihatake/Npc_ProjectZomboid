package personal.knox.alife;

/** Simple squad tactics: cover, ambush and retreat without splitting the squad. */
public final class AlifeTactics {
    public record Plan(String stance, double coverX, double coverY, boolean ambush, boolean retreat) {}
    private AlifeTactics() {}

    public static Plan plan(int members, int wounded, int morale, int enemyStrength, boolean hasCover) {
        boolean retreat = wounded * 2 >= Math.max(1, members) || morale < 30;
        boolean ambush = !retreat && hasCover && enemyStrength <= members;
        String stance = retreat ? "retreat" : ambush ? "ambush" : hasCover ? "cover" : "hold";
        return new Plan(stance, hasCover ? 1 : 0, hasCover ? 1 : 0, ambush, retreat);
    }

    public static boolean lineClear(boolean allyInFront) { return !allyInFront; }
}
