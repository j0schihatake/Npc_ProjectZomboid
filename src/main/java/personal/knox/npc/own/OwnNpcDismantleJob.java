package personal.knox.npc.own;

import java.util.Set;

/** Timed dismantle action; no world access. */
public final class OwnNpcDismantleJob {
    private final OwnNpcToolUse.Plan plan;
    private double remaining;

    private OwnNpcDismantleJob(OwnNpcToolUse.Plan plan) {
        this.plan = plan;
        this.remaining = plan.seconds();
    }

    public static OwnNpcDismantleJob start(Set<OwnNpcToolUse.Tool> tools, OwnNpcToolUse.Obstacle obstacle) {
        return new OwnNpcDismantleJob(OwnNpcToolUse.plan(tools, obstacle));
    }

    public double tick(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0) throw new IllegalArgumentException("Invalid time");
        remaining = Math.max(0, remaining - seconds);
        return remaining;
    }

    public boolean done() { return remaining <= 0; }
    public double remaining() { return remaining; }
    public double noise() { return plan.noise(); }
    public double wear() { return plan.wear(); }
    public OwnNpcToolUse.Obstacle obstacle() { return plan.obstacle(); }
}
