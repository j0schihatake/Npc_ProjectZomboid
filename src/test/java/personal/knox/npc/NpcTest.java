package personal.knox.npc;

public final class NpcTest {
    private static NpcState state(double hunger, double thirst, double fatigue, NpcState.Task task) {
        return new NpcState(100, hunger, thirst, fatigue, 0.5, false, false,
                NpcState.Role.GUARD, task);
    }

    public static void run() {
        var base = state(0, 0, 0, NpcState.Task.FOLLOW);
        var ticked = NpcNeeds.tick(base, 10);
        assert Math.abs(ticked.hunger() - 0.12) < 1e-9;
        assert Math.abs(ticked.thirst() - 0.18) < 1e-9;
        assert Math.abs(ticked.fatigue() - 0.10) < 1e-9;
        var full = NpcNeeds.tick(state(0.99, 0.99, 0.99, NpcState.Task.REST), 100);
        assert full.hunger() == 1 && full.thirst() == 1 && full.fatigue() == 1 : "Needs clamp at one";
        try { NpcNeeds.tick(base, -1); throw new AssertionError("Negative hours accepted"); }
        catch (IllegalArgumentException expected) { }
        try { NpcNeeds.tick(null, 1); throw new AssertionError("Null state accepted"); }
        catch (IllegalArgumentException expected) { }

        assert NpcDecision.next(base, false, true, true, true) == NpcDecision.Action.FOLLOW;
        assert NpcDecision.next(base, true, true, true, true) == NpcDecision.Action.FIGHT;
        var hurt = base.withBleeding(true);
        assert NpcDecision.next(hurt, false, true, true, true) == NpcDecision.Action.BANDAGE;
        assert NpcDecision.next(hurt, false, false, true, true) != NpcDecision.Action.BANDAGE;
        assert NpcDecision.next(state(0, .7, 0, NpcState.Task.WORK), false, true, true, true) == NpcDecision.Action.DRINK;
        assert NpcDecision.next(state(.7, 0, 0, NpcState.Task.WORK), false, true, true, true) == NpcDecision.Action.EAT;
        assert NpcDecision.next(state(0, 0, .9, NpcState.Task.WORK), false, true, true, true) == NpcDecision.Action.REST;
        var downed = new NpcState(10, .9, .9, .9, 0.1, true, true, NpcState.Role.MEDIC, NpcState.Task.FIGHT);
        assert NpcDecision.next(downed, true, true, true, true) == NpcDecision.Action.FLEE;
        assert NpcDecision.next(downed, false, true, true, true) == NpcDecision.Action.REST;
        assert NpcDecision.next(state(0, 0, 0, NpcState.Task.GUARD), false, false, false, false) == NpcDecision.Action.GUARD;
        try { new NpcState(101, 0, 0, 0, 0, false, false, NpcState.Role.GUARD, NpcState.Task.IDLE); throw new AssertionError("Bad health accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new NpcState(100, 1.1, 0, 0, 0, false, false, NpcState.Role.GUARD, NpcState.Task.IDLE); throw new AssertionError("Bad need accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("NPC model tests passed: need decay, clamps, danger/fight, bandage, drink, eat, rest, downed, tasks and validation.");
    }
}
