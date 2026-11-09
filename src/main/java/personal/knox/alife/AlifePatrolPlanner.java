package personal.knox.alife;

import java.util.ArrayList;
import java.util.List;

/** Ring patrol route that can avoid protected cells. */
public final class AlifePatrolPlanner {
    private AlifePatrolPlanner() {}

    public static List<double[]> ring(double x, double y, double radius, int count) {
        int n = Math.max(2, Math.min(32, count));
        double r = Math.max(1, radius);
        var points = new ArrayList<double[]>();
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n;
            points.add(new double[]{x + Math.cos(angle) * r, y + Math.sin(angle) * r});
        }
        return List.copyOf(points);
    }

    public static List<double[]> avoid(List<double[]> points, List<double[]> blocked, double clearance) {
        var allowed = new ArrayList<double[]>();
        for (var point : points) {
            boolean safe = true;
            for (var b : blocked)
                if (Math.hypot(point[0] - b[0], point[1] - b[1]) < clearance) { safe = false; break; }
            if (safe) allowed.add(point);
        }
        return List.copyOf(allowed);
    }

    public static String summary(double x, double y, double radius) {
        var points = ring(x, y, radius, 8);
        var blocked = List.of(new double[]{x + radius, y});
        var safe = avoid(points, blocked, 1);
        return "route=" + points.size() + " safe=" + safe.size();
    }
}
