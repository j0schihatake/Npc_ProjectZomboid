package personal.knox.alife;

/** Claimed territory that refuses protected player cells. */
public final class AlifeTerritory {
    public record Claim(String owner, double x, double y, double radius) {}
    private AlifeTerritory() {}

    public static boolean canClaim(double x, double y, double radius, java.util.List<double[]> protectedCells) {
        if (!Double.isFinite(x + y + radius) || radius <= 0) return false;
        for (var cell : protectedCells) {
            if (Math.hypot(x - cell[0], y - cell[1]) <= radius) return false;
        }
        return true;
    }

    public static boolean overlaps(Claim a, Claim b) {
        return Math.hypot(a.x() - b.x(), a.y() - b.y()) < a.radius() + b.radius();
    }

    public static Claim claim(String owner, double x, double y, double radius, java.util.List<double[]> protectedCells) {
        return canClaim(x, y, radius, protectedCells) ? new Claim(owner, x, y, radius) : null;
    }
}
