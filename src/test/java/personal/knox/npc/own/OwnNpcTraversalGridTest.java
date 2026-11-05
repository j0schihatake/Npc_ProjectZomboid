package personal.knox.npc.own;

public final class OwnNpcTraversalGridTest {
    public static void run() {
        var grid = new OwnNpcTraversalGrid(new OwnNpcTraversalGrid.World() {
            public boolean passable(int x, int y) { return x >= 0; }
            public OwnNpcTraversal.Kind traversalAt(int x, int y) {
                if (x == 1) return OwnNpcTraversal.Kind.WINDOW;
                if (x == 2) return OwnNpcTraversal.Kind.HIGH_FENCE;
                if (x == 3) return OwnNpcTraversal.Kind.ROPE;
                return null;
            }
        });
        assert grid.passable(0, 0) && !grid.passable(-1, 0);
        assert grid.cost(0, 0) == 0;
        assert grid.cost(1, 0) == 1.2 && grid.cost(2, 0) == 2.0 && grid.cost(3, 0) == 3.0;
        try { new OwnNpcTraversalGrid(null); throw new AssertionError("Null world accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Own-NPC traversal-grid tests passed: passability and window/fence/rope costs.");
    }
}
