package personal.knox.npc.own;

public final class OwnNpcPathfinderTest {
    public static void run() {
        var open = new OwnNpcPathfinder.Grid() {
            public boolean passable(int x, int y) { return true; }
        };
        var straight = OwnNpcPathfinder.find(0, 0, 3, 0, open, 100);
        assert straight.size() == 4 && straight.get(3).x() == 3;
        var diagonal = OwnNpcPathfinder.find(0, 0, 2, 2, open, 100);
        assert diagonal.size() == 3 && diagonal.get(2).equals(new OwnNpcPathfinder.Point(2, 2)) : diagonal;
        var wall = new OwnNpcPathfinder.Grid() {
            public boolean passable(int x, int y) { return !(x == 1 && y != 2); }
        };
        var detour = OwnNpcPathfinder.find(0, 0, 3, 0, wall, 200);
        assert detour.size() > 4 && detour.stream().anyMatch(p -> p.y() == 2);
        var blocked = new OwnNpcPathfinder.Grid() {
            public boolean passable(int x, int y) { return false; }
        };
        assert OwnNpcPathfinder.find(0, 0, 3, 0, blocked, 100).isEmpty();
        assert OwnNpcPathfinder.find(0, 0, 50, 50, open, 10).isEmpty() : "Budget holds";
        System.out.println("Own-NPC pathfinder tests passed: straight, wall detour, blocked and budget.");
    }
}
