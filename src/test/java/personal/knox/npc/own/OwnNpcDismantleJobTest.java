package personal.knox.npc.own;

import java.util.EnumSet;

public final class OwnNpcDismantleJobTest {
    public static void run() {
        var job = OwnNpcDismantleJob.start(EnumSet.of(OwnNpcToolUse.Tool.HAMMER), OwnNpcToolUse.Obstacle.FENCE);
        assert job.remaining() == 6 && !job.done() && job.noise() == 4;
        assert Math.abs(job.tick(2) - 4) < 1e-9 && !job.done();
        assert Math.abs(job.tick(4) - 0) < 1e-9 && job.done();
        try { job.tick(0); throw new AssertionError("Zero time accepted"); }
        catch (IllegalArgumentException expected) { }
        try { OwnNpcDismantleJob.start(EnumSet.noneOf(OwnNpcToolUse.Tool.class), OwnNpcToolUse.Obstacle.WALL); throw new AssertionError("No tools accepted"); }
        catch (IllegalStateException expected) { }
        var fast = OwnNpcDismantleJob.start(EnumSet.of(OwnNpcToolUse.Tool.HAMMER), OwnNpcToolUse.Obstacle.FENCE, 10);
        assert fast.remaining() == 3;
        System.out.println("Own-NPC dismantle-job tests passed: countdown, done, noise and invalid time.");
    }
}
