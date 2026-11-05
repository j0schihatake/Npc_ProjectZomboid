package personal.knox.npc.own;

public final class OwnNpcTraversalTest {
    public static void run() {
        assert OwnNpcTraversal.canUse(OwnNpcTraversal.Kind.WINDOW, false, true, false, 1, true, false);
        assert !OwnNpcTraversal.canUse(OwnNpcTraversal.Kind.WINDOW, false, true, false, 1, false, false);
        assert OwnNpcTraversal.canUse(OwnNpcTraversal.Kind.HIGH_FENCE, false, true, false, 1, false, true);
        assert !OwnNpcTraversal.canUse(OwnNpcTraversal.Kind.HIGH_FENCE, false, true, true, 1, false, true);
        assert OwnNpcTraversal.canUse(OwnNpcTraversal.Kind.ROPE, true, true, false, 0.5, false, false);
        assert !OwnNpcTraversal.canUse(OwnNpcTraversal.Kind.ROPE, false, true, false, 0.5, false, false);
        assert OwnNpcTraversal.plan(OwnNpcTraversal.Kind.HIGH_FENCE, false).seconds() > 1;
        try { OwnNpcTraversal.plan(OwnNpcTraversal.Kind.ROPE, false); throw new AssertionError("Rope without rope accepted"); }
        catch (IllegalStateException expected) { }
        System.out.println("Own-NPC traversal tests passed: window, fence, rope, hands, injury and rope requirement.");
    }
}
