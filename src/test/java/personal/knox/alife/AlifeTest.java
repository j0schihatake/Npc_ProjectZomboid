package personal.knox.alife;

import java.util.List;

public final class AlifeTest {
    private static Squad squad(String id, String faction, double x, double y, Squad.Task task, int supplies) {
        return new Squad(id, faction, 4, 100, supplies, task, x, y, 0, x + 100, y);
    }

    public static void run() {
        var faction = new Faction("own", "Свои", Faction.Stance.NEUTRAL, 0);
        assert faction.id().equals("own");
        try { new Faction("", "x", Faction.Stance.NEUTRAL, 0); throw new AssertionError("Empty id accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new Faction("x", "x", Faction.Stance.NEUTRAL, 101); throw new AssertionError("Bad reputation accepted"); }
        catch (IllegalArgumentException expected) { }

        try { new Squad("s", "f", 17, 100, 1, Squad.Task.HOLD, 0, 0, 0, 0, 0); throw new AssertionError("Too many members"); }
        catch (IllegalArgumentException expected) { }
        try { new Squad("s", "f", 1, Double.NaN, 1, Squad.Task.HOLD, 0, 0, 0, 0, 0); throw new AssertionError("NaN health accepted"); }
        catch (IllegalArgumentException expected) { }

        var relations = new Relations();
        relations.record("raiders", Relations.Event.ATTACKED, 0);
        assert relations.score("raiders") == -25;
        assert relations.stance("raiders") == Faction.Stance.HOSTILE;
        assert !relations.wantsRevenge("raiders", Relations.DAY) : "Too early for revenge";
        assert relations.wantsRevenge("raiders", 3 * Relations.DAY);
        assert !relations.wantsRevenge("raiders", 8 * Relations.DAY) : "Grudge must expire";
        relations.record("raiders", Relations.Event.HELPED, 3 * Relations.DAY);
        assert relations.score("raiders") == -5;
        assert !relations.wantsRevenge("raiders", 4 * Relations.DAY) : "Help cancels revenge";
        relations.record("friends", Relations.Event.HELPED, 0);
        assert relations.stance("friends") == Faction.Stance.NEUTRAL : "+20 is still neutral";
        relations.record("friends", Relations.Event.HELPED, 0);
        assert relations.stance("friends") == Faction.Stance.FRIENDLY;
        var beforeDecay = relations.score("friends");
        relations.decay(20 * Relations.DAY);
        assert relations.score("friends") < beforeDecay && relations.score("friends") >= 0 : "Decay moves to neutral";
        try { relations.record("x", Relations.Event.ATTACKED, -1); throw new AssertionError("Negative time accepted"); }
        catch (IllegalArgumentException expected) { }

        var world = new AlifeWorld(42);
        world.addSquad(squad("near", "own", 0, 0, Squad.Task.HOLD, 5));
        world.addSquad(squad("far1", "raiders", 200, 0, Squad.Task.PATROL, 5));
        world.addSquad(squad("far2", "raiders", 300, 0, Squad.Task.RAID, 5));
        var first = world.tick(1000, 0, 0, 0, 2);
        assert first.size() == 2 : "Budget must cap work";
        assert first.get(0).type() == AlifeEvent.Type.NEAR_SQUAD;
        assert first.get(1).type() == AlifeEvent.Type.FAR_MOVE;
        assert Math.abs(world.squad("far1").x() - 208) < 1e-9 : "Far squad moves toward target";
        assert Math.abs(world.squad("far2").x() - 300) < 1e-9 : "Unprocessed squad stays unchanged";
        var second = world.tick(2000, 0, 0, 0, 3);
        assert second.size() == 3;
        assert Math.abs(world.squad("far2").x() - 308) < 1e-9;
        try { world.tick(500, 0, 0, 0, 1); throw new AssertionError("Time moved backwards"); }
        catch (IllegalArgumentException expected) { }
        try { world.tick(3000, Double.NaN, 0, 0, 1); throw new AssertionError("NaN position accepted"); }
        catch (IllegalArgumentException expected) { }

        var returning = new AlifeWorld(1);
        returning.addSquad(squad("low", "raiders", 500, 0, Squad.Task.SCAVENGE, 1));
        var events = returning.tick(1000, 0, 0, 0, 5);
        assert events.stream().anyMatch(event -> event.type() == AlifeEvent.Type.FAR_RETURN) : "Empty supplies must return";

        var left = new AlifeWorld(7);
        var right = new AlifeWorld(7);
        for (var target : List.of(left, right)) {
            target.addSquad(squad("a", "raiders", 300, 0, Squad.Task.PATROL, 9));
            target.addSquad(squad("b", "own", 400, 0, Squad.Task.PATROL, 9));
        }
        var leftEvents = left.tick(5000, 0, 0, 0, 10);
        var rightEvents = right.tick(5000, 0, 0, 0, 10);
        assert leftEvents.equals(rightEvents) : "Same seed and input must give the same events";
        assert left.squad("a").equals(right.squad("a"));
        try { returning.addSquad(squad("low", "raiders", 0, 0, Squad.Task.HOLD, 1)); throw new AssertionError("Duplicate squad accepted"); }
        catch (IllegalArgumentException expected) { }
        var outpost = new Outpost("camp", "own", Outpost.Kind.CAMP, 10, 20, 0, 4, 3, true);
        assert outpost.canDispatch();
        assert !outpost.withRadio(false).canDispatch() : "Radio off stops dispatch";
        assert !outpost.withSupplies(0).canDispatch() : "No supplies stops dispatch";
        assert !outpost.withSupplies(2).withRadio(false).canDispatch();
        try { new Outpost("", "own", Outpost.Kind.CAMP, 0, 0, 0, 1, 1, true); throw new AssertionError("Empty id accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new Outpost("x", "own", Outpost.Kind.CAMP, 0, 0, 0, 65, 1, true); throw new AssertionError("Too many guards"); }
        catch (IllegalArgumentException expected) { }

        var camp = new AlifeWorld(9);
        camp.addOutpost(new Outpost("camp", "own", Outpost.Kind.CAMP, 10, 20, 0, 4, 1, true));
        assert camp.outpostCount() == 1 && camp.canDispatch("camp");
        var campEvents = camp.tick(1000, 500, 500, 0, 10);
        assert campEvents.stream().anyMatch(e -> e.type() == AlifeEvent.Type.OUTPOST_SILENT) : "Last supply cuts the radio";
        assert !camp.canDispatch("camp");
        assert camp.outpost("camp").supplies() == 0 && !camp.outpost("camp").radio();
        try { camp.addOutpost(new Outpost("camp", "own", Outpost.Kind.CAMP, 0, 0, 0, 1, 1, true)); throw new AssertionError("Duplicate outpost accepted"); }
        catch (IllegalArgumentException expected) { }
        assert camp.removeOutpost("camp") != null && camp.outpostCount() == 0;
        personal.knox.alife.DynamicRelationsTest.run();
        personal.knox.alife.FactionPresenceTest.run();
        System.out.println("A-Life model tests passed: factions, squads, relations, revenge, budget, determinism.");
    }
}
