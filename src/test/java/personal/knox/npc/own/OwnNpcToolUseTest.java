package personal.knox.npc.own;

import java.util.EnumSet;

public final class OwnNpcToolUseTest {
    public static void run() {
        assert OwnNpcToolUse.canDismantle(EnumSet.of(OwnNpcToolUse.Tool.SCREWDRIVER), OwnNpcToolUse.Obstacle.WINDOW);
        assert !OwnNpcToolUse.canDismantle(EnumSet.of(OwnNpcToolUse.Tool.HAMMER), OwnNpcToolUse.Obstacle.WINDOW);
        assert OwnNpcToolUse.seconds(EnumSet.of(OwnNpcToolUse.Tool.HAMMER), OwnNpcToolUse.Obstacle.FENCE) == 6;
        assert OwnNpcToolUse.required(OwnNpcToolUse.Obstacle.WALL).size() == 2;
        try { OwnNpcToolUse.seconds(EnumSet.noneOf(OwnNpcToolUse.Tool.class), OwnNpcToolUse.Obstacle.WALL); throw new AssertionError("Wall without tools accepted"); }
        catch (IllegalStateException expected) { }
        System.out.println("Own-NPC tool-use tests passed: window, fence, wall, missing tools and invalid state.");
    }
}
