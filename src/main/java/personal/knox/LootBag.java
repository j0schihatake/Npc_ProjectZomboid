package personal.knox;

/** State machine for a bag carried on a loot run. No game objects. */
public final class LootBag {
    public enum State { CARRIED, DROPPED, RECOVERED, DELIVERED, LOST }

    private final String id;
    private State state = State.CARRIED;
    private double x, y, z;

    public LootBag(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Missing bag id");
        this.id = id;
    }

    public String id() { return id; }
    public State state() { return state; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }

    public void drop(double x, double y, double z) {
        require(State.CARRIED, "drop");
        if (!Double.isFinite(x + y + z)) throw new IllegalArgumentException("Invalid drop position");
        this.x = x;
        this.y = y;
        this.z = z;
        state = State.DROPPED;
    }

    public void recover() {
        require(State.DROPPED, "recover");
        state = State.RECOVERED;
    }

    public void deliver() {
        if (state != State.CARRIED && state != State.RECOVERED) throw new IllegalStateException("Cannot deliver " + state);
        state = State.DELIVERED;
    }

    public void lose() {
        if (state == State.DELIVERED || state == State.LOST) throw new IllegalStateException("Cannot lose " + state);
        state = State.LOST;
    }

    private void require(State expected, String action) {
        if (state != expected) throw new IllegalStateException("Cannot " + action + " " + state);
    }
}
