package personal.knox;

import java.util.List;

/** Chooses which enemy to shoot first when covering a wounded ally. No game objects. */
public final class ShotPriority {
    public record Point(double x, double y) {
        public Point {
            if (!Double.isFinite(x) || !Double.isFinite(y)) throw new IllegalArgumentException("Invalid point");
        }
    }

    private ShotPriority() {}

    /**
     * Primary key: distance to the protected ally. Tie-break: distance to the shooter.
     * Enemies outside the shooter's range are ignored.
     */
    public static int choose(Point ally, Point shooter, List<Point> enemies, double maxRange) {
        if (ally == null || shooter == null || enemies == null || enemies.isEmpty()
                || !Double.isFinite(maxRange) || maxRange <= 0) return -1;
        int best = -1;
        double bestAlly = Double.POSITIVE_INFINITY;
        double bestShooter = Double.POSITIVE_INFINITY;
        for (int i = 0; i < enemies.size(); i++) {
            var enemy = enemies.get(i);
            if (enemy == null) continue;
            double toShooter = Math.hypot(enemy.x() - shooter.x(), enemy.y() - shooter.y());
            if (toShooter > maxRange) continue;
            double toAlly = Math.hypot(enemy.x() - ally.x(), enemy.y() - ally.y());
            if (toAlly < bestAlly || (toAlly == bestAlly && toShooter < bestShooter)) {
                best = i;
                bestAlly = toAlly;
                bestShooter = toShooter;
            }
        }
        return best;
    }
}
