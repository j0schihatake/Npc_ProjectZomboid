package personal.knox.npc.own;

import personal.knox.npc.NpcState;

public final class OwnNpcLayerTest {
    public static void run() {
        var spawnLayer=new OwnNpcLayer();
        String first=spawnLayer.debugSpawn(10.5,20.5,2).agentId();
        String second=spawnLayer.debugSpawn(11.5,20.5,2).agentId();
        assert spawnLayer.agent(first).x()==10.5 && spawnLayer.agent(first).z()==2;
        spawnLayer.remove(first);
        String replacement=spawnLayer.debugSpawn(12.5,20.5,2).agentId();
        assert !replacement.equals(second) && spawnLayer.count()==2 : "Deleting a body must not cause duplicate IDs";

        var layer = new OwnNpcLayer();
        var state = new NpcState(100, 0.1, 0.1, 0.1, 0.8, false, false,
                NpcState.Role.SCAVENGER, NpcState.Task.FOLLOW);
        var agent = new OwnNpcAgent("n1", "own", 0, 0, 0, 10, 0, 2, state);
        assert layer.spawn(agent).type() == OwnNpcEvent.Type.SPAWNED;
        assert layer.count() == 1;
        layer.order("n1", OwnNpcOrder.FOLLOW, 10, 0);
        layer.followTarget(10, 0);
        var events = layer.tick(1, 8);
        assert !events.isEmpty();
        assert layer.agent("n1").x() > 0 : "Agent moved toward target";
        assert layer.agent("n1").state().hunger() > state.hunger() : "Needs decayed";
        layer.order("n1", OwnNpcOrder.FOLLOW, 50, 0);
        layer.followTarget(60, 60);
        layer.tick(1, 8);
        assert layer.agent("n1").targetX() == 60 && layer.agent("n1").targetY() == 60;
        assert layer.agent("n1").x() > 0 : "FOLLOW moves toward follow target";

        layer.order("n1", 50, 0);
        assert layer.agent("n1").targetX() == 50;
        var formation = new OwnNpcLayer();
        var formationIds = new String[4];
        for (int i = 0; i < formationIds.length; i++) {
            formationIds[i] = formation.debugSpawn(i, 0, 0).agentId();
            formation.order(formationIds[i], OwnNpcOrder.FOLLOW, 20, 20);
        }
        formation.followTarget(20, 20);
        formation.tick(0.01, 32);
        var targets = new java.util.HashSet<String>();
        for (var id : formationIds) {
            targets.add(Math.round(formation.agent(id).targetX()) + "," + Math.round(formation.agent(id).targetY()));
        }
        assert targets.size() > 1 : "Follow targets must form a spread, not one pile";
        var healer = new OwnNpcAgent("n4", "own", 0, 0, 0, 0, 0, 2,
                new NpcState(50, 0.1, 0.1, 0.1, 0.5, false, false, NpcState.Role.GUARD, NpcState.Task.GUARD));
        layer.spawn(healer);
        layer.tick(1, 8);
        assert layer.agent("n4").state().health() > 50 : "Health regenerates";
        layer.remove("n4");
        var bleeder = new OwnNpcAgent("n3", "own", 0, 0, 0, 0, 0, 1,
                new NpcState(60, 0.1, 0.1, 0.1, 0.5, true, false,
                        NpcState.Role.GUARD, NpcState.Task.GUARD));
        layer.spawn(bleeder);
        layer.tick(0.01, 8);
        assert !layer.agent("n3").state().bleeding() : "Bleeding treated";
        layer.remove("n3");
        assert layer.damage("n1", 10).type() == OwnNpcEvent.Type.NEEDS;
        assert layer.damage("n1", 200).type() == OwnNpcEvent.Type.DIED;
        assert layer.agent("n1") == null;
        layer.restore(OwnNpcCodec.decode(OwnNpcCodec.encode(java.util.List.of(agent))));
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
