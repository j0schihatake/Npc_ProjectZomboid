package personal.knox.npc.own;

import personal.knox.npc.NpcState;

public final class OwnNpcLayerTest {
    public static void run() {
        var layer = new OwnNpcLayer();
        var state = new NpcState(100, 0.1, 0.1, 0.1, 0.8, false, false,
                NpcState.Role.SCAVENGER, NpcState.Task.FOLLOW);
        var agent = new OwnNpcAgent("n1", "own", 0, 0, 0, 10, 0, 2, state);
        assert layer.spawn(agent).type() == OwnNpcEvent.Type.SPAWNED;
        assert layer.count() == 1;
        var events = layer.tick(1, 8);
        assert !events.isEmpty();
        assert layer.agent("n1").x() > 0 : "Agent moved toward target";
        assert layer.agent("n1").state().hunger() > state.hunger() : "Needs decayed";
        layer.order("n1", OwnNpcOrder.FOLLOW, 50, 0);
        layer.followTarget(60, 60);
        layer.tick(1, 8);
        assert layer.agent("n1").targetX() == 60 && layer.agent("n1").targetY() == 60;
        layer.order("n1", 50, 0);
        assert layer.agent("n1").targetX() == 50;
        var bleeder = new OwnNpcAgent("n3", "own", 0, 0, 0, 0, 0, 1,
                new NpcState(60, 0.1, 0.1, 0.1, 0.5, true, false,
                        NpcState.Role.GUARD, NpcState.Task.GUARD));
        layer.spawn(bleeder);
        layer.tick(0.01, 8);
        assert !layer.agent("n3").state().bleeding() : "Bleeding treated";
        layer.remove("n3");
        var hungryState = new NpcState(100, 0.9, 0.9, 0.1, 0.8, false, false,
                NpcState.Role.SCAVENGER, NpcState.Task.FOLLOW);
        var hungry = new OwnNpcAgent("n2", "own", 0, 0, 0, 0, 0, 2, hungryState);
        layer.spawn(hungry);
        layer.tick(0.01, 8);
        assert layer.agent("n2").state().hunger() < 0.9 : "Hunger reduced by supplies";
        assert layer.agent("n2").supplies() < 2 : "Supplies consumed";
        layer.remove("n2");
        assert layer.remove("n1").type() == OwnNpcEvent.Type.REMOVED;
        assert layer.count() == 0;
        var saved = OwnNpcCodec.encode(java.util.List.of(agent));
        layer.restore(OwnNpcCodec.decode(saved));
        assert layer.count() == 1 && layer.agent("n1") != null;
        try { layer.spawn(agent); layer.spawn(agent); throw new AssertionError("Duplicate accepted"); }
        catch (IllegalArgumentException expected) { }
        try { layer.remove("missing"); throw new AssertionError("Unknown removal accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Own-NPC tests passed: spawn, bounded tick, movement, needs, removal and invalid input.");
    }
}
