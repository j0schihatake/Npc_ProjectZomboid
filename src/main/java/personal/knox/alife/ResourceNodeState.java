package personal.knox.alife;

/** Mutable depletion/refresh state for a resource node. */
public final class ResourceNodeState {
    private final ResourceNode node;
    private int remaining;
    private boolean active = true;
    private long refreshAt;

    public ResourceNodeState(ResourceNode node) {
        if (node == null) throw new IllegalArgumentException("Missing node");
        this.node = node;
        this.remaining = node.value();
    }

    public ResourceNode node() { return node; }
    public int remaining() { return remaining; }
    public boolean active() { return active; }
    public long refreshAt() { return refreshAt; }

    /** Returns how much was actually harvested; depletes the node. */
    public int harvest(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Invalid harvest");
        if (!active) return 0;
        int taken = Math.min(remaining, amount);
        remaining -= taken;
        if (remaining == 0) active = false;
        return taken;
    }

    public void scheduleRefresh(long nowMillis, long delayMillis) {
        if (nowMillis < 0 || delayMillis <= 0) throw new IllegalArgumentException("Invalid refresh");
        if (active) throw new IllegalStateException("Node is not depleted");
        refreshAt = nowMillis + delayMillis;
    }

    public boolean tryRefresh(long nowMillis) {
        if (active || refreshAt == 0 || nowMillis < refreshAt) return false;
        remaining = node.value();
        active = true;
        refreshAt = 0;
        return true;
    }
}
