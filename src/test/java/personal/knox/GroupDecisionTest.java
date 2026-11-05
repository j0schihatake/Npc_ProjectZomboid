package personal.knox;

public final class GroupDecisionTest {
    private static GroupDecision.Vote vote(GroupDecision.Choice choice, double weight) {
        return new GroupDecision.Vote(choice, weight);
    }

    public static void run() {
        assert GroupDecision.decide(java.util.List.of(), null) == GroupDecision.Choice.HOLD;
        assert GroupDecision.decide(null, null) == GroupDecision.Choice.HOLD;
        assert GroupDecision.decide(java.util.List.of(vote(GroupDecision.Choice.ATTACK, 2)), null)
                == GroupDecision.Choice.ATTACK;
        assert GroupDecision.decide(java.util.List.of(
                vote(GroupDecision.Choice.ATTACK, 1), vote(GroupDecision.Choice.RETREAT, 2)), null)
                == GroupDecision.Choice.RETREAT : "Weighted majority";
        assert GroupDecision.decide(java.util.List.of(
                vote(GroupDecision.Choice.ATTACK, 1), vote(GroupDecision.Choice.RETREAT, 1)), null)
                == GroupDecision.Choice.RETREAT : "Tie falls back to retreat";
        assert GroupDecision.decide(java.util.List.of(
                vote(GroupDecision.Choice.ATTACK, 5), vote(GroupDecision.Choice.RETREAT, 1)),
                GroupDecision.Choice.HOLD) == GroupDecision.Choice.HOLD : "Leader overrides votes";
        try { vote(null, 1); throw new AssertionError("Null choice accepted"); }
        catch (IllegalArgumentException expected) { }
        try { vote(GroupDecision.Choice.ATTACK, 0); throw new AssertionError("Zero weight accepted"); }
        catch (IllegalArgumentException expected) { }
        try { GroupDecision.decide(java.util.Arrays.asList((GroupDecision.Vote) null), null); throw new AssertionError("Null vote accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Group decision tests passed: leader override, weighted majority, tie retreat, empty and invalid votes.");
    }
}
