package personal.knox;

import java.util.List;

/** Own line-of-fire check: never shoot through a friendly. No game objects. */
public final class LineOfFire {
    private LineOfFire() {}

    /** Fails closed: invalid geometry is treated as blocked. */
    public static boolean blocked(ShotPriority.Point shooter, ShotPriority.Point target,
                                  List<ShotPriority.Point> allies, double radius) {
        if (shooter == null || target == null || !Double.isFinite(radius) || radius < 0) return true;
        if (allies == null || allies.isEmpty()) return false;
        for (var ally : allies) {
            if (ally == null) continue;
            if (distanceToSegment(ally, shooter, target) <= radius) return true;
        }
        return false;
    }

    private static double distanceToSegment(ShotPriority.Point point, ShotPriority.Point a, ShotPriority.Point b) {
        double dx = b.x() - a.x(), dy = b.y() - a.y();
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared < 1e-12) return Math.hypot(point.x() - a.x(), point.y() - a.y());
        double t = ((point.x() - a.x()) * dx + (point.y() - a.y()) * dy) / lengthSquared;
        if (t < 0) return Math.hypot(point.x() - a.x(), point.y() - a.y());
        if (t > 1) return Math.hypot(point.x() - b.x(), point.y() - b.y());
        double projectionX = a.x() + t * dx, projectionY = a.y() + t * dy;
        return Math.hypot(point.x() - projectionX, point.y() - projectionY);
    }
}
