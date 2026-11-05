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

        var presenceWorld = new AlifeWorld(13);
        presenceWorld.addFactionPresence(new FactionPresence.State("own", 2, true, 100));
        presenceWorld.setRespawnDelayMillis(1000);
        presenceWorld.loseFactionSquad("own", 1400);
        var returnEvents = presenceWorld.tick(2500, 0, 0, 0, 10);
        assert returnEvents.stream().anyMatch(e -> e.type() == AlifeEvent.Type.FACTION_RETURN) : "Faction return event";
        assert presenceWorld.factionPresence("own").members() == 1;

        presenceWorld.loseFactionSquad("own", 200);
        assert presenceWorld.factionPresence("own").members() == 0;
        assert presenceWorld.respawnReady(300, 1000).isEmpty();
        assert presenceWorld.respawnReady(1200, 1000).contains("own");
        presenceWorld.respawnFaction("own", 3, 1300);
        assert presenceWorld.factionPresence("own").members() == 3;
        try { presenceWorld.addFactionPresence(new FactionPresence.State("own", 1, true, 0)); throw new AssertionError("Duplicate presence accepted"); }
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
        personal.knox.alife.FactionEconomyTest.run();
        var expanding = new AlifeWorld(19);
        var rich = new FactionEconomy(2);
        rich.add(FactionEconomy.Resource.FOOD, 100);
        rich.add(FactionEconomy.Resource.WATER, 100);
        rich.add(FactionEconomy.Resource.MATERIALS, 100);
        expanding.addFactionEconomy("rich", rich);
        assert expanding.tick(0, 0, 0, 0, 10).stream().anyMatch(e -> e.type() == AlifeEvent.Type.FACTION_EXPAND);
        assert expanding.tick(1000, 0, 0, 0, 10).stream().noneMatch(e -> e.type() == AlifeEvent.Type.FACTION_EXPAND);
        var nodeWorld = new AlifeWorld(21);
        nodeWorld.addResourceNode(new ResourceNode("farm", ResourceNode.Kind.FOOD, 10, 10, 0, "neutral", 10));
        var captured = nodeWorld.captureResourceNode("farm", "own", 100);
        assert captured.type() == AlifeEvent.Type.NODE_CAPTURED;
        assert nodeWorld.resourceNode("farm").controller().equals("own");
        var incomeWorld = new AlifeWorld(23);
        var incomeEconomy = new FactionEconomy(0);
        incomeWorld.addFactionEconomy("own", incomeEconomy);
        incomeWorld.addResourceNode(new ResourceNode("farm2", ResourceNode.Kind.FOOD, 0, 0, 0, "own", 7));
        for (long i = 0; i < 7; i++) incomeWorld.tick(i * 60_000L, 0, 0, 0, 10);
        assert incomeEconomy.get(FactionEconomy.Resource.FOOD) >= 7;
        var economyWorld = new AlifeWorld(17);
        economyWorld.addFactionEconomy("hungry", new FactionEconomy(2));
        var starvingEvents = economyWorld.tick(100, 0, 0, 0, 10);
        assert starvingEvents.stream().anyMatch(e -> e.type() == AlifeEvent.Type.FACTION_STARVING);
        personal.knox.alife.BaseLevelTest.run();
        personal.knox.alife.ResourceNodeStateTest.run();
        personal.knox.alife.BaseDefenseTest.run();
        personal.knox.alife.RaidPolicyTest.run();
        personal.knox.alife.RaidTest.run();
        personal.knox.alife.TheftPolicyTest.run();
        personal.knox.alife.AssaultResolverTest.run();
        var raidWorld = new AlifeWorld(29);
        var raid = new Raid("r1", "raiders", Raid.TargetKind.BASE, "farm", 30, 0, Raid.State.MARCHING);
        raidWorld.startRaid(raid);
        var outcome = raidWorld.resolveRaid("r1", 0.9, 2, 0.2, 50);
        assert outcome.attackersWin();
        assert raidWorld.raids().get(0).state() == Raid.State.WON;
        System.out.println("A-Life model tests passed: factions, squads, relations, revenge, budget, determinism.");
    }
}
