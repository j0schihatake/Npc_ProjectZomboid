package personal.knox.npc.own;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/** Deterministic bounded A* for own NPCs; traversal costs come from the game adapter. */
public final class OwnNpcPathfinder {
    public record Point(int x, int y) {}
    public interface Grid {
        boolean passable(int x, int y);
        default boolean canStep(int fx, int fy, int tx, int ty) { return passable(tx, ty); }
        default double cost(int x, int y) { return 0; }
    }

    private OwnNpcPathfinder() {}

    public static List<Point> find(int sx, int sy, int tx, int ty, Grid grid, int maxNodes) {
        if (grid == null || maxNodes <= 0) throw new IllegalArgumentException("Invalid path request");
        if (!grid.passable(sx, sy) || !grid.passable(tx, ty)) return List.of();
        var start = new Point(sx, sy);
        var target = new Point(tx, ty);
        var open = new PriorityQueue<Node>((a, b) -> Double.compare(a.f, b.f));
        var best = new HashMap<Point, Double>();
        var closed = new HashSet<Point>();
        var came = new HashMap<Point, Point>();
        open.add(new Node(start, 0, heuristic(start, target)));
        best.put(start, 0.0);
        int visited = 0;
        while (!open.isEmpty() && visited < maxNodes) {
            var current = open.poll();
            if (!closed.add(current.point)) continue;
            visited++;
            if (current.point.equals(target)) return reconstruct(came, target);
            for (var step : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                var next = new Point(current.point.x + step[0], current.point.y + step[1]);
                if (closed.contains(next) || !grid.canStep(current.point.x, current.point.y, next.x, next.y)) continue;
                double g = current.g + 1.0 + grid.cost(next.x, next.y);
                if (g >= best.getOrDefault(next, Double.MAX_VALUE)) continue;
                best.put(next, g);
                came.put(next, current.point);
                open.add(new Node(next, g, g + heuristic(next, target)));
            }
        }
        return List.of();
    }

    private static List<Point> reconstruct(Map<Point, Point> came, Point target) {
        var path = new ArrayList<Point>();
        var current = target;
        while (current != null) {
            path.add(current);
            current = came.get(current);
        }
        Collections.reverse(path);
        return List.copyOf(path);
    }

    private static double heuristic(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    private record Node(Point point, double g, double f) {}
}
