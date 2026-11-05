package personal.knox.quest;

public final class QuestTest {
    public static void run() {
        var offered = new Quest("q1", Quest.Kind.RESCUE, "own", "survivor", 3, Quest.State.OFFERED);
        assert QuestPolicy.canAccept(offered);
        var active = QuestPolicy.accept(offered);
        assert active.state() == Quest.State.ACTIVE;
        assert QuestPolicy.complete(active).state() == Quest.State.COMPLETED;
        assert QuestPolicy.fail(active).state() == Quest.State.FAILED;
        assert QuestPolicy.decline(offered).state() == Quest.State.DECLINED;
        assert QuestPolicy.relationDelta(Quest.State.DECLINED) == 0;
        assert QuestPolicy.relationDelta(Quest.State.FAILED) < 0;
        assert QuestPolicy.relationDelta(Quest.State.COMPLETED) > 0;
        try { QuestPolicy.accept(active); throw new AssertionError("Active quest re-accepted"); }
        catch (IllegalStateException expected) { }
        try { new Quest("q", Quest.Kind.SEARCH, "own", "x", 0, Quest.State.OFFERED); throw new AssertionError("Zero stages accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Quest tests passed: accept, complete, fail, decline, relation deltas and invalid transitions.");
    }
}
