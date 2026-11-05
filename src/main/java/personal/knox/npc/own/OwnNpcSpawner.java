package personal.knox.npc.own;

/** Promotes/demotes groups by distance to the player; no world scans. */
public final class OwnNpcSpawner {
    private OwnNpcSpawner() {}

    public static int sync(OwnNpcPopulation population, OwnNpcLayer layer,
                           double px, double py, double radius, int perGroup) {
        if (population == null || layer == null || perGroup < 0
                || !Double.isFinite(px + py) || !Double.isFinite(radius) || radius < 0)
            throw new IllegalArgumentException("Invalid spawn sync");
        int active = 0;
        for (var group : population.groups()) {
            double distance = Math.hypot(group.x() - px, group.y() - py);
            if (distance <= radius) {
                active += OwnNpcPromotion.promote(population, layer, group.id(), perGroup);
            } else {
                OwnNpcPromotion.demote(population, layer, group.id(), 0);
            }
        }
        return active;
    }
}
