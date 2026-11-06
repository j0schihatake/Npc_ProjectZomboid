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
        var plan = OwnNpcToolUse.plan(EnumSet.of(OwnNpcToolUse.Tool.HAMMER), OwnNpcToolUse.Obstacle.FENCE);
        assert plan.seconds() == 6 && plan.noise() == 4 && plan.wear() == 1;
        assert OwnNpcToolUse.fromItems(java.util.List.of("Base.Hammer", "Base.Screwdriver"))
                .equals(EnumSet.of(OwnNpcToolUse.Tool.HAMMER, OwnNpcToolUse.Tool.SCREWDRIVER));
        assert OwnNpcToolUse.classify("zombie.iso.objects.IsoWindow") == OwnNpcToolUse.Obstacle.WINDOW;
        assert OwnNpcToolUse.classify("IsoBarricade") == OwnNpcToolUse.Obstacle.BARRICADE;
        try { OwnNpcToolUse.classify("tree"); throw new AssertionError("Unknown obstacle accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Own-NPC tool-use tests passed: window, fence, wall, missing tools and invalid state.");
    }
}
