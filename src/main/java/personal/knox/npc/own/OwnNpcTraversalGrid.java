package personal.knox.npc.own;

/** Adapts traversal features (window/fence/rope) to A* costs. */
public final class OwnNpcTraversalGrid implements OwnNpcPathfinder.Grid {
    public interface World {
        boolean passable(int x, int y);
        default boolean canStep(int fx, int fy, int tx, int ty) { return passable(tx, ty); }
        OwnNpcTraversal.Kind traversalAt(int x, int y); // null means normal floor
    }

    private final World world;

    public OwnNpcTraversalGrid(World world) {
        if (world == null) throw new IllegalArgumentException("Missing world");
        this.world = world;
    }

    @Override public boolean passable(int x, int y) { return world.passable(x, y); }

    @Override public boolean canStep(int fx, int fy, int tx, int ty) { return world.canStep(fx, fy, tx, ty); }

    @Override public double cost(int x, int y) {
        var kind = world.traversalAt(x, y);
        if (kind == null) return 0;
        return switch (kind) {
            case WINDOW -> 1.2;
            case HIGH_FENCE -> 2.0;
            case ROPE -> 3.0;
            case STAIRS -> 1.0;
        };
    }
}
