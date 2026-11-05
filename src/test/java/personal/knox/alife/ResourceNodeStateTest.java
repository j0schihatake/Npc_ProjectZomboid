package personal.knox.alife;

public final class ResourceNodeStateTest {
    public static void run() {
        var state = new ResourceNodeState(new ResourceNode("farm", ResourceNode.Kind.FOOD, 0, 0, 0, "own", 5));
        assert state.harvest(2) == 2 && state.remaining() == 3 && state.active();
        assert state.harvest(100) == 3 && !state.active();
        assert state.harvest(1) == 0;
        state.scheduleRefresh(100, 1000);
        assert !state.tryRefresh(500);
        assert state.tryRefresh(1200);
        assert state.active() && state.remaining() == 5;
        try { state.harvest(-1); throw new AssertionError("Negative harvest accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new ResourceNodeState(null); throw new AssertionError("Null node accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Resource-node state tests passed: harvest, depletion, refresh delay and invalid input.");
    }
}
